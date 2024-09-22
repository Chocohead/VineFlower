package org.teavm.samples.hello;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

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
    private static HTMLElement responsePanel = document.getElementById("response-panel");
    private static HTMLElement thinkingPanel = document.getElementById("thinking-panel");

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
        thinkingPanel.getStyle().setProperty("display", "");

        JSArray<JSPromise<File>> files = new JSArray<>(/*input.getFiles().getLength()*/);
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

        			System.out.println(saved);
        			return JSPromise.resolve(saved);
        		} catch (IOException e) {
        			return JSPromise.reject(file.getName() + " crashed: " + e.getMessage());
        		}
        	}));
        }

        JSPromise.all(files).then(Client::startDecompile, error -> {
        	responsePanel.appendChild(document.createElement("pre", paragraph -> {
        		paragraph.setInnerText(error.toString());
        		paragraph.setClassName("error");
        	}));
        	return null;
        }).onSettled(() -> {
        	decompileButton.setDisabled(false);
        	thinkingPanel.getStyle().setProperty("display", "none");
        	return null;
        });
    }

    private static Void startDecompile(JSArrayReader<File> files) {
    	Builder builder = Decompiler.builder();

    	for (File file : Utils.iterate(files)) {
    		builder.inputs(file);
    	}

    	new Thread(() -> {
    	Map<String, String> results = new ConcurrentHashMap<>();
    	System.out.println(Thread.currentThread());
    	builder.output(new StringyFileSaver(results)).build().decompile();

        for (Entry<String, String> entry : results.entrySet()) {
        	responsePanel.appendChild(document.createElement("h2", header -> {
        		header.setInnerText(entry.getKey());
        	}));
        	responsePanel.appendChild(document.createElement("pre", paragraph -> {
        		paragraph.setInnerText(entry.getValue());
        	}));
        }
    	}).start();

    	return null;
    }
}