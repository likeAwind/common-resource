package com.windforce.common.resource.support;

import java.util.Map;

import org.springframework.core.convert.converter.Converter;

import com.windforce.common.utility.JsonUtils;

public class StringToMapConverter implements Converter<String, Map<String, Object>> {

	@Override
	public Map<String, Object> convert(String source) {
		return JsonUtils.string2Map(source);
	}

}
