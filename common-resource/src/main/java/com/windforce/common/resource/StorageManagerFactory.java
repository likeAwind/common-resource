package com.windforce.common.resource;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;

import com.windforce.common.resource.other.ResourceDefinition;
import com.windforce.common.resource.reader.ReaderHolder;
import com.windforce.common.resource.reader.ResourceReader;

/**
 * 资源管理器工厂
 * @author frank
 */
public class StorageManagerFactory implements FactoryBean<StorageManager>, ApplicationContextAware {

	/** 资源定义列表 */
	private List<ResourceDefinition> definitions;

	public void setDefinitions(List<ResourceDefinition> definitions) {
		this.definitions = definitions;
	}

	@Override
	public StorageManager getObject() throws Exception {
		StorageManager result = this.applicationContext.getAutowireCapableBeanFactory()
				.createBean(StorageManager.class);
		ExecutorService service = Executors.newFixedThreadPool(definitions.size());
		CompletionService<ResourceDefinition> completionService = new ExecutorCompletionService<ResourceDefinition>(
				service);
		ReaderHolder readerHolder = applicationContext.getBean(ReaderHolder.class);

		for (ResourceDefinition definition : definitions) {
			completionService.submit(new Callable<ResourceDefinition>() {
				public ResourceDefinition call() throws Exception {
					ResourceReader reader = readerHolder.getReader(definition.getFormat());
					Resource resource = applicationContext.getResource(definition.getLocation());
					InputStream input = resource.getInputStream();
					List<?> startList = reader.read(input, definition.getClz());
					definition.setStartList(startList);
					return definition;
				}
			});
		}

		for (int i = 0; i < definitions.size(); i++) {
			completionService.take();
		}
		service.shutdown();

		for (ResourceDefinition definition : definitions) {
			result.initialize(definition);
		}

		return result;
	}

	// 实现接口的方法

	@Override
	public Class<StorageManager> getObjectType() {
		return StorageManager.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	private ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

}
