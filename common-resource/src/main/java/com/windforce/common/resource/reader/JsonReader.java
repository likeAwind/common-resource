package com.windforce.common.resource.reader;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

import com.alibaba.fastjson.JSON;
import com.windforce.common.resource.exception.DecodeException;

/**
 * 
 * @author Kuang Hao
 * @since v1.0 2018年2月8日
 *
 */
public class JsonReader implements ResourceReader {

	public <E> List<E> read(InputStream input, Class<E> clz) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] buf = new byte[1024];
			int hasRead = 0;
			while ((hasRead = input.read(buf)) != -1) {
				baos.write(buf, 0, hasRead);
			}
			String msg = baos.toString();
			return JSON.parseArray(msg, clz);
		} catch (Exception e) {
			throw new DecodeException(e);
		}
	}

	@Override
	public String getFormat() {
		return "json";
	}

}
