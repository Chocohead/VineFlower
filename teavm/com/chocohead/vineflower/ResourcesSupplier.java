package com.chocohead.vineflower;

import org.teavm.classlib.ResourceSupplier;
import org.teavm.classlib.ResourceSupplierContext;

public class ResourcesSupplier implements ResourceSupplier {
	@Override
	public String[] supplyResources(ResourceSupplierContext context) {
		//return new String[] {"classes.txt"};
		return new String[] {"java.zip"};
	}
}