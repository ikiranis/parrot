package eu.apps4net.parrotApp.utilities;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;

public class ReflectionUtils {

	/**
	 * Update selected fields of an object
	 *
	 * @param targetClass    The object to update
	 * @param source         The source map
	 * @param selectedFields The fields to update
	 * @throws IllegalAccessException
	 */
	public static void updateFields(Object targetClass, Map<String, String> source, String[] selectedFields) throws IllegalAccessException {
		Class<?> clazz = targetClass.getClass();

		for (Field field : clazz.getDeclaredFields()) {

			if (source.containsKey(field.getName())) {
				field.setAccessible(true);
				Object newValue = source.get(field.getName());
				Object currentValue = field.get(targetClass);

				if (!newValue.equals(currentValue)) {
					if (Arrays.asList(selectedFields).contains(field.getName())) {
						field.set(targetClass, newValue);
					}
				}
			}
		}
	}
}
