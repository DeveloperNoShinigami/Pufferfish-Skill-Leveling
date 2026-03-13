package net.bluelotuscoding.skillleveling.util;

import java.lang.reflect.Method;
import java.util.Arrays;

public class MethodInspector {
    public static void inspect(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            System.out.println("Methods for " + className + ":");
            for (Method m : clazz.getMethods()) {
                System.out.println(m.getName() + " " + Arrays.toString(m.getParameterTypes()) + " -> " + m.getReturnType());
            }
            System.out.println("\nFields for " + className + ":");
            for (java.lang.reflect.Field f : clazz.getDeclaredFields()) {
                System.out.println(f.getName() + " : " + f.getType());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
