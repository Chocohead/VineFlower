package com.chocohead.vineflower;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.teavm.jso.JSBody;
import org.teavm.jso.browser.Window;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSArrayReader;
import org.teavm.jso.core.JSPromise;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.html.HTMLButtonElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLInputElement;
import org.teavm.jso.typedarrays.Int8Array;

import org.jetbrains.java.decompiler.api.Decompiler;
import org.jetbrains.java.decompiler.api.Decompiler.Builder;

public final class Client {
	private static HTMLDocument document = Window.current().getDocument();
	private static HTMLInputElement input = document.getElementById("input").cast();
	private static HTMLButtonElement decompileButton = document.getElementById("decompile-button").cast();
	private static HTMLElement progressPanel = document.getElementById("progress-panel");
	private static HTMLElement navigationPanel = document.getElementById("navigation-panel");
	private static HTMLElement outputPanel = document.getElementById("output-panel");
	private static Results rootResults = new Results("");

	private Client() {
	}

	public static void main(String[] args) {
		try (InputStream in = Client.class.getResourceAsStream("/java.zip"); OutputStream out = new FileOutputStream("java.zip")) {
			in.transferTo(out);
		} catch (IOException e) {
			System.err.println("Failed to copy JRE classes");
			e.printStackTrace();
		}

		input.addEventListener("change", Client::inputChange);
		decompileButton.onClick(Client::startDecompile);
	}

	private static void inputChange(Event event) {
		decompileButton.setDisabled(input.getFiles().getLength() == 0);
	}

	private static void startDecompile(Event event) {
		decompileButton.setDisabled(true);
		progressPanel.getStyle().setProperty("visibility", "visible");

		JSArray<JSPromise<File>> files = new JSArray<>();
		for (org.teavm.jso.file.File file : Utils.iterate(input.getFiles())) {
			files.push(file.arrayBuffer().flatThen(buffer -> {
				Int8Array contents = new Int8Array(buffer);
				try {
					File saved = new File(file.getName());

					try (OutputStream out = new FileOutputStream(saved)) {
						for (int i = 0, end = contents.getByteLength(); i < end; i++) {
							out.write(contents.get(i)); //Dribble the file in
						}
					}

					return JSPromise.resolve(saved);
				} catch (IOException e) {
					return JSPromise.reject(file.getName() + " crashed: " + e.getMessage());
				}
			}));
		}

		JSPromise.all(files).then(Client::startDecompile).catchError(error -> {
			try {
				outputPanel.clear().withChild("pre", paragraph -> {
					paragraph.setInnerText(Objects.toString(error));
					paragraph.setClassName("error");
				});
			} finally {
				decompileButton.setDisabled(false);
				progressPanel.getStyle().setProperty("visibility", "hidden");
			}
			return null;
		});
	}

	private static Void startDecompile(JSArrayReader<File> files) {
		Builder builder = Decompiler.builder();

		for (File file : Utils.iterate(files)) {
			builder.inputs(file);
		}

		new Thread(() -> {
			try {
				Map<String, String> results = new ConcurrentHashMap<>();
				builder.output(new StringyFileSaver(results)).build().decompile();

				for (Entry<String, String> entry : results.entrySet()) {
					String[] packages = entry.getKey().split("/");

					Results root = rootResults;
					for (int i = 0, end = packages.length - 2; i < end; i++) {
						root = root.children().computeIfAbsent(packages[i], Results::new);
					}
					root.classes().put(packages[packages.length - 1].split("\\.")[0], entry.getValue());
				}

				buildResultsTree(navigationPanel.clear(), rootResults);
			} catch (Throwable t) {
				StringWriter crash = new StringWriter();
				t.printStackTrace(new PrintWriter(crash));
				outputPanel.clear().withChild("pre", paragraph -> {
					paragraph.setInnerText(crash.toString());
					paragraph.setClassName("error");
				});
			} finally {
				decompileButton.setDisabled(false);
				progressPanel.getStyle().setProperty("visibility", "hidden");
			}
		}).start();

		return null;
	}

	private static void buildResultsTree(HTMLElement panel, Results root) {
		for (Results child : root.children().values()) {
			panel.withChild("li", li -> {
				li.withChild("span", span -> {
					span.setInnerText(child.name());
					span.addEventListener("click", event -> {
						li.getClassList().toggle("open");
					});
				}).withChild("ul", ul -> {
					buildResultsTree(ul, child);
				});
			});
		}
		for (Entry<String, String> entry : root.classes().entrySet()) {
			panel.withChild("li", li -> {
				li.setInnerText(entry.getKey());
				li.addEventListener("click", event -> switchTo(entry.getValue()));
			});
		}
	}

	private static void switchTo(String content) {
		outputPanel.clear().withChild("pre", pre -> {
			pre.withChild("code", code -> {
				code.setTextContent(content);
				code.setClassName("language-java");
				highlightElement(code);
			});
		});
	}

	@JSBody(params = {"element"}, script = "hljs.highlightElement(element)")
	public static native void highlightElement(HTMLElement element);
}