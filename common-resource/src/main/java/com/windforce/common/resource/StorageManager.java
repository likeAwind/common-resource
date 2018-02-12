package com.windforce.common.resource;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.windforce.common.resource.other.ResourceDefinition;
import com.windforce.common.utility.JsonUtils;

/**
 * 资源管理器
 * 
 * @author frank
 */
@SuppressWarnings("rawtypes")
public class StorageManager implements ApplicationContextAware {

	private static final Logger logger = LoggerFactory.getLogger(StorageManager.class);

	/** 静态类资源定义 */
	private ConcurrentHashMap<Class, ResourceDefinition> definitions = new ConcurrentHashMap<Class, ResourceDefinition>();
	/** 资源存储空间 */
	private ConcurrentHashMap<Class<?>, Storage<?, ?>> storages = new ConcurrentHashMap<Class<?>, Storage<?, ?>>();
	/** 缓存文件 */
	private static ConcurrentHashMap<String, InputStream> caches = new ConcurrentHashMap<String, InputStream>();

	/**
	 * 初始化静态类资源
	 * 
	 * @param definition
	 *            资源定义
	 */
	public void initialize(ResourceDefinition definition) {
		Class<?> clz = definition.getClz();
		if (definitions.putIfAbsent(clz, definition) != null) {
			ResourceDefinition prev = definitions.get(clz);
			FormattingTuple message = MessageFormatter.format("类[{}]的资源定义[{}]已经存在", clz, prev);
			logger.error(message.getMessage());
			throw new RuntimeException(message.getMessage());
		}
		initializeStorage(clz);
	}

	/**
	 * 重新加载静态类资源
	 * 
	 * @param clz
	 *            要重新加载的类资源
	 */
	public void reload(Class<?> clz) {
		ResourceDefinition definition = definitions.get(clz);
		if (definition == null) {
			FormattingTuple message = MessageFormatter.format("类[{}]的资源定义不存在", clz);
			logger.error(message.getMessage());
			throw new RuntimeException(message.getMessage());
		}

		Storage<?, ?> storage = getStorage(clz);
		storage.reload();
	}

	@SuppressWarnings("unchecked")
	public <T> T getResource(Object key, Class<T> clz) {
		Storage storage = getStorage(clz);
		return (T) storage.get(key, false);
	}

	/**
	 * 获取指定类资源的存储空间
	 * 
	 * @param clz
	 *            类实例
	 * @return
	 */
	public Storage<?, ?> getStorage(Class clz) {
		if (storages.containsKey(clz)) {
			return storages.get(clz);
		}
		return initializeStorage(clz);
	}

	/**
	 * 获取全部的存储空间对象数组
	 * 
	 * @return
	 */
	public Storage<?, ?>[] listStorages() {
		List<Storage<?, ?>> storages = new ArrayList<Storage<?, ?>>();
		for (Storage<?, ?> storage : this.storages.values()) {
			storages.add(storage);
		}
		return storages.toArray(new Storage[0]);
	}

	/**
	 * 初始化类资源的存储空间
	 * 
	 * @param clz
	 *            类实例
	 * @return
	 */
	private Storage initializeStorage(Class clz) {
		ResourceDefinition definition = this.definitions.get(clz);
		if (definition == null) {
			FormattingTuple message = MessageFormatter.format("静态资源[{}]的信息定义不存在，可能是配置缺失", clz.getSimpleName());
			logger.error(message.getMessage());
			throw new IllegalStateException(message.getMessage());
		}
		AutowireCapableBeanFactory beanFactory = this.applicationContext.getAutowireCapableBeanFactory();
		Storage storage = beanFactory.createBean(Storage.class);

		Storage prev = storages.putIfAbsent(clz, storage);
		if (prev == null) {
			storage.initialize(definition);
		}
		return prev == null ? storage : prev;
	}

	public void writeJson(String path) throws Exception {
		for (Entry<Class<?>, Storage<?, ?>> entry : storages.entrySet()) {
			try {
				long startTime = System.currentTimeMillis();
				String json = JsonUtils.object2String(entry.getValue().getAll());
				// logger.error(json);
				File file = new File(String.format(path + "\\%s.json", entry.getKey().getSimpleName()));
				createFile(file);
				FileWriter fw = new FileWriter(file);
				fw.write(json);
				fw.flush();
				fw.close();

				long endTime = System.currentTimeMillis();
				System.out.println("处理完成" + entry.getKey().getSimpleName() + " 耗时：" + (endTime - startTime) + " ms");

			} catch (Exception e) {
				e.printStackTrace();
				System.out.println(entry.getKey().getSimpleName() + " 有问题！");
			}
		}
	}

	private boolean createFile(File file) throws Exception {
		if (!file.exists()) {
			mkdir(file.getParentFile());
		}
		return file.createNewFile();
	}

	private void mkdir(File file) {
		if (!file.getParentFile().exists()) {
			mkdir(file.getParentFile());
		}
		file.mkdir();
	}

	// 实现接口的方法

	private ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	public static void putCache(String key, InputStream inputStream) {
		caches.put(key, inputStream);
	}

	public static InputStream getFromCache(String key) {
		return caches.get(key);
	}

}
