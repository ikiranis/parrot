package eu.apps4net.parrotApp.utilities;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;

/**
 * Utility class providing reflection-based helper methods for updating
 * entity fields from a string-keyed source map.
 */
public class ReflectionUtils {

	/**
	 * Updates the selected fields of {@code targetClass} with values from {@code source}.
	 * Only fields whose names appear in both {@code source} and {@code selectedFields}
	 * are updated; fields whose current value already matches the new value are skipped.
	 *
	 * @param targetClass    the object whose fields should be updated
	 * @param source         map of field name to new string value
	 * @param selectedFields whitelist of field names that may be modified
	 * @throws IllegalAccessException if a field cannot be accessed via reflection
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
