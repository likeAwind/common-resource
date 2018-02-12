package com.windforce.common.resource.spring;

import org.springframework.core.convert.ConversionService;

import com.windforce.common.resource.anno.Id;
import com.windforce.common.resource.anno.Inject;
import com.windforce.common.resource.anno.Resource;

@Resource
public class Config {

	@Inject
	private static ConversionService conversionService;

	@Id
	private String id;
	private String value;

	@Inject("testBean")
	private InjectObject injectObject;

	public ConversionService getConversionService() {
		return conversionService;
	}

	public InjectObject getInjectObject() {
		return injectObject;
	}

	// Getter and Setter ...

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

}
