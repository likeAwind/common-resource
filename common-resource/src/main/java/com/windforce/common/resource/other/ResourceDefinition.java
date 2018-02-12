package com.windforce.common.resource.other;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.springframework.util.ReflectionUtils.FieldCallback;
import org.springframework.util.ReflectionUtils.FieldFilter;

import com.windforce.common.resource.anno.Inject;
import com.windforce.common.resource.anno.Resource;
import com.windforce.common.utility.ReflectionUtility;

/**
 * 资源定义信息对象
 * 
 * @author frank
 */
public class ResourceDefinition {

	public final static String FILE_SPLIT = ".";
	public final static String FILE_PATH = File.separator;

	/** 注入属性域过滤器 */
	private final static FieldFilter INJECT_FILTER = new FieldFilter() {
		@Override
		public boolean matches(Field field) {
			if (field.isAnnotationPresent(Inject.class)) {
				return true;
			}
			return false;
		}
	};

	/** 资源类 */
	private final Class<?> clz;
	/** 资源路径 */
	private final String location;
	/** 资源格式 */
	private final String format;
	/** 资源的注入信息 */
	private final Set<InjectDefinition> injects = new HashSet<InjectDefinition>();

	private String cacheKey;

	public ResourceDefinition(Class<?> clz, FormatDefinition format) {
		this.clz = clz;
		this.format = format.getType();
		Resource anno = clz.getAnnotation(Resource.class);
		if (StringUtils.isBlank(anno.value())) {
			String name = clz.getSimpleName();
			this.location = format.getLocation() + FILE_PATH + name + FILE_SPLIT + format.getSuffix();
		} else {
			String name = anno.value();
			if (StringUtils.startsWith(name, FILE_PATH)) {
				name = StringUtils.substringAfter(name, FILE_PATH);
			}
			this.location = format.getLocation() + FILE_PATH + name + FILE_SPLIT + format.getSuffix();
		}
		if (!StringUtils.isBlank(anno.cache())) {
			cacheKey = anno.cache();
		}
		ReflectionUtility.doWithDeclaredFields(clz, new FieldCallback() {
			@Override
			public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
				InjectDefinition definition = new InjectDefinition(field);
				injects.add(definition);
			}
		}, INJECT_FILTER);
	}

	/**
	 * 获取静态属性注入定义
	 * 
	 * @return
	 */
	public Set<InjectDefinition> getStaticInjects() {
		HashSet<InjectDefinition> result = new HashSet<InjectDefinition>();
		for (InjectDefinition definition : this.injects) {
			Field field = definition.getField();
			if (Modifier.isStatic(field.getModifiers())) {
				result.add(definition);
			}
		}
		return result;
	}

	/**
	 * 获取非静态属性注入定义
	 * 
	 * @return
	 */
	public Set<InjectDefinition> getInjects() {
		HashSet<InjectDefinition> result = new HashSet<InjectDefinition>();
		for (InjectDefinition definition : this.injects) {
			Field field = definition.getField();
			if (!Modifier.isStatic(field.getModifiers())) {
				result.add(definition);
			}
		}
		return result;
	}

	// Getter and Setter ...

	public Class<?> getClz() {
		return clz;
	}

	public String getLocation() {
		return location;
	}

	public String getFormat() {
		return format;
	}

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this);
	}

	public String getCacheKey() {
		return cacheKey;
	}
}
