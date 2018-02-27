package com.windforce.common.resource.reader;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.windforce.common.utility.JsonUtils;
import com.windforce.common.utility.ReflectionUtility;

/**
 * excel类型转换器
 * @author Kuang Hao
 * @since v1.0 2018年2月27日
 *
 */
public class ExcelReader implements ResourceReader {

	private final static Logger logger = LoggerFactory.getLogger(ExcelReader.class);

	/** 服务端控制标识同时也是数据开始标识 */
	private final static String ROW_SERVER = "SERVER";
	/** 结束标识 */
	private final static String ROW_END = "END";

	// private final static TypeDescriptor sourceType =
	// TypeDescriptor.valueOf(String.class);

	@Override
	public String getFormat() {
		return "excel";
	}

	/**
	 * 属性信息
	 * 
	 * @author frank
	 */
	private static class FieldInfo {
		/** 第几列 */
		public final int index;
		/** 资源类属性 */
		public final Field field;

		/** 构造方法 */
		public FieldInfo(int index, Field field) {
			ReflectionUtility.makeAccessible(field);
			this.index = index;
			this.field = field;
		}
	}

	@Override
	public <E> List<E> read(InputStream input, Class<E> clz) {
		// 基本信息获取
		Workbook wb = getWorkbook(input, clz);
		Sheet[] sheets = getSheets(wb, clz);

		// 创建返回数据集
		ArrayList<E> result = new ArrayList<E>();
		for (Sheet sheet : sheets) {
			Collection<FieldInfo> infos = getCellInfos(sheet, clz);
			boolean start = false;
			for (Row row : sheet) {
				// 判断数据行开始没有
				if (!start) {
					Cell cell = row.getCell(0);
					if (cell == null) {
						continue;
					}
					String content = getCellContent(cell);
					if (content == null) {
						continue;
					}
					if (content.equals(ROW_SERVER)) {
						start = true;
					}
					continue;
				}
				// 生成返回对象
				E instance = newInstance(clz);
				for (FieldInfo info : infos) {
					Cell cell = row.getCell(info.index);
					if (cell == null) {
						continue;
					}
					String content = getCellContent(cell);
					if (StringUtils.isEmpty(content)) {
						continue;
					}
					setValue(instance, info.field, content);
				}
				result.add(instance);

				// 结束处理
				Cell cell = row.getCell(0);
				if (cell == null) {
					continue;
				}
				String content = getCellContent(cell);
				if (content == null) {
					continue;
				}
				if (content.equals(ROW_END)) {
					break;
				}
			}
		}
		return result;
	}

	private void setValue(Object instance, Field field, String content) {
		try {

			if (field.getType() == String.class) {
				field.set(instance, content);
				return;
			}
			if (field.getType() == int.class || field.getType() == Integer.class) {
				field.set(instance, Integer.valueOf(content));
				return;
			}
			if (field.getType() == long.class || field.getType() == Long.class) {
				field.set(instance, Long.valueOf(content));
				return;
			}
			if (field.getType() == boolean.class || field.getType() == Boolean.class) {
				field.set(instance, Boolean.valueOf(content));
				return;
			}
			if (field.getType() == byte.class || field.getType() == Byte.class) {
				field.set(instance, Byte.valueOf(content));
				return;
			}
			if (content.startsWith("[") || content.startsWith("{")) {
				// lis对象
				if (field.getType() == java.util.List.class) {
					Class<?>[] clazzs = getParameterizedType(field);
					List<?> list = JsonUtils.string2List(content, clazzs[0]);
					field.set(instance, list);
					return;
				}

				// Map对象
				if (field.getType() == java.util.Map.class) {
					Class<?>[] clazzs = getParameterizedType(field);
					Map<Object, Object> newMap = new HashMap<>();
					if (clazzs[0] == Integer.class) {
						TypeReference<Map<Integer, String>> mapType = new TypeReference<Map<Integer, String>>() {
						};
						Map<Integer, String> map = JSON.parseObject(content, mapType);
						for (Entry<Integer, String> entry : map.entrySet()) {
							Object key = null;
							key = Integer.valueOf(entry.getKey());
							Object value = JsonUtils.string2Object(entry.getValue(), clazzs[1]);
							newMap.put(key, value);
						}
					} else if (clazzs[0] == String.class) {
						TypeReference<Map<String, String>> mapType = new TypeReference<Map<String, String>>() {
						};
						Map<String, String> map = JSON.parseObject(content, mapType);
						for (Entry<String, String> entry : map.entrySet()) {
							Object key = null;
							key = String.valueOf(entry.getKey());
							Object value = JsonUtils.string2Object(entry.getValue(), clazzs[1]);
							newMap.put(key, value);
						}
					} else {
						String message = String.format("静态资源[%s]属性[%s],属性类型[%s],Map中key的值必须为Integer或者String!",
								instance.getClass().getSimpleName(), field.getName(), field.getType().getName());
						logger.error(message);
						throw new IllegalStateException(message);
					}
					field.set(instance, newMap);
					return;
				}

				// 对象
				Object object = JsonUtils.string2Object(content, field.getType());
				field.set(instance, object);
				return;
			}

			String message = String.format("没有找到对应的静态资源[%s]属性[%s],属性类型[%s],内容[%s]的转换处理内容!",
					instance.getClass().getSimpleName(), field.getName(), field.getType().getName(), content);
			logger.error(message);
			throw new IllegalStateException(message);

		} catch (Exception e) {
			String message = String.format("静态资源[%s]属性[%s],属性类型[%s],内容[%s]的转换失败!", instance.getClass().getSimpleName(),
					field.getName(), field.getType().getName(), content);
			logger.error(message, e);
			throw new IllegalStateException(message, e);
		}
	}

	public static Class<?>[] getParameterizedType(Field f) {

		// 获取f字段的通用类型
		Type fc = f.getGenericType(); // 关键的地方得到其Generic的类型

		// 如果不为空并且是泛型参数的类型
		if (fc != null && fc instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) fc;

			Type[] types = pt.getActualTypeArguments();

			if (types != null && types.length > 0) {
				Class<?>[] classes = new Class<?>[types.length];
				for (int i = 0; i < classes.length; i++) {
					classes[i] = (Class<?>) types[i];
				}
				return classes;
			}
		}
		return null;
	}

	/**
	 * 获取字符串形式的单元格内容
	 * 
	 * @param cell
	 * @return
	 */
	private String getCellContent(Cell cell) {
		if (cell.getCellType() != Cell.CELL_TYPE_STRING) {
			cell.setCellType(Cell.CELL_TYPE_STRING);
		}
		return cell.getStringCellValue();
	}

	/*
	@Autowired
	private ConversionService conversionService;
	 */
	/**
	 * 给实例注入属性
	 * 
	 * @param instance
	 * @param field
	 * @param content
	 */
	/*
	@SuppressWarnings("unused")
	@Deprecated
	private void inject(Object instance, Field field, String content) {
		// Class<?> clz = field.getType();
		try {
			TypeDescriptor targetType = new TypeDescriptor(field);
			Object value = conversionService.convert(content, sourceType, targetType);
			field.set(instance, value);
		} catch (ConverterNotFoundException e) {
			FormattingTuple message = MessageFormatter.format("静态资源[{}]属性[{}]的转换器不存在",
					instance.getClass().getSimpleName(), field.getName());
			logger.error(message.getMessage(), e);
			throw new IllegalStateException(message.getMessage(), e);
		} catch (Exception e) {
			FormattingTuple message = MessageFormatter.format("属性[{}]注入失败", field);
			logger.error(message.getMessage());
			throw new IllegalStateException(message.getMessage(), e);
		}
	}
	*/

	/**
	 * 实例化资源
	 * 
	 * @param <E>
	 * @param clz
	 * @return
	 */
	private <E> E newInstance(Class<E> clz) {
		try {
			return clz.newInstance();
		} catch (Exception e) {
			FormattingTuple message = MessageFormatter.format("资源[{}]无法实例化", clz);
			logger.error(message.getMessage());
			throw new RuntimeException(message.getMessage());
		}
	}

	/**
	 * 获取表格信息
	 * 
	 * @param sheet
	 * @param clz
	 * @return
	 */
	private Collection<FieldInfo> getCellInfos(Sheet sheet, Class<?> clz) {
		// 获取属性控制行
		Row fieldRow = getFieldRow(sheet);
		if (fieldRow == null) {
			FormattingTuple message = MessageFormatter.format("无法获取资源[{}]的EXCEL文件的属性控制列", clz);
			logger.error(message.getMessage());
			throw new IllegalStateException(message.getMessage());
		}

		// 获取属性信息集合
		List<FieldInfo> result = new ArrayList<FieldInfo>();
		for (int i = 1; i < fieldRow.getLastCellNum(); i++) {
			Cell cell = fieldRow.getCell(i);
			if (cell == null) {
				continue;
			}

			String name = getCellContent(cell);
			if (StringUtils.isBlank(name)) {
				continue;
			}

			try {
				Field field = clz.getDeclaredField(name);
				FieldInfo info = new FieldInfo(i, field);
				result.add(info);
			} catch (Exception e) {
				FormattingTuple message = MessageFormatter.format("资源类[{}]的声明属性[{}]无法获取", clz, name);
				logger.error(message.getMessage());
				throw new IllegalStateException(message.getMessage(), e);
			}
		}
		return result;
	}

	/**
	 * 获取属性控制行
	 * 
	 * @param sheet
	 * @return
	 */
	private Row getFieldRow(Sheet sheet) {
		for (Row row : sheet) {
			Cell cell = row.getCell(0);
			if (cell == null) {
				continue;
			}
			String content = getCellContent(cell);
			if (content != null && content.equals(ROW_SERVER)) {
				return row;
			}
		}
		return null;
	}

	/**
	 * 获取资源类型对应的工作簿
	 * 
	 * @param wb
	 *            Excel Workbook
	 * @param clz
	 *            资源类型
	 * @return
	 */
	private Sheet[] getSheets(Workbook wb, Class<?> clz) {
		try {
			List<Sheet> result = new ArrayList<Sheet>();
			String name = clz.getSimpleName();
			// 处理多Sheet数据合并
			for (int i = 0; i < wb.getNumberOfSheets(); i++) {
				Sheet sheet = wb.getSheetAt(i);
				if (sheet.getLastRowNum() <= 0) {
					continue;
				}
				Row row = sheet.getRow(0);
				if (row.getLastCellNum() <= 0) {
					continue;
				}
				Cell cell = row.getCell(0);
				if (cell == null) {
					continue;
				}
				if (cell.getCellType() != Cell.CELL_TYPE_STRING) {
					cell.setCellType(Cell.CELL_TYPE_STRING);
				}
				String text = cell.getStringCellValue();
				if (name.equals(text)) {
					result.add(sheet);
				}
			}
			if (result.size() > 0) {
				return result.toArray(new Sheet[0]);
			}

			// 没有需要多Sheet合并的情况
			Sheet sheet = wb.getSheet(name);
			if (sheet != null) {
				return new Sheet[] { sheet };
			} else {
				return new Sheet[] { wb.getSheetAt(0) };
			}
		} catch (Exception e) {
			throw new IllegalArgumentException("无法获取资源类[" + clz.getSimpleName() + "]对应的Excel数据表", e);
		}
	}

	/**
	 * 通过输入流获取{@link Workbook}
	 * 
	 * @param input
	 * @return
	 */
	private Workbook getWorkbook(InputStream input, @SuppressWarnings("rawtypes") Class clz) {
		try {
			return WorkbookFactory.create(input);
		} catch (InvalidFormatException e) {
			throw new RuntimeException("静态资源[" + clz.getSimpleName() + "]异常,无效的文件格式", e);
		} catch (IOException e) {
			throw new RuntimeException("静态资源[" + clz.getSimpleName() + "]异常,无法读取文件", e);
		}
	}

}
