package com.mall.common.util;

import com.mall.domain.user.entity.SysUser;
import com.mall.dto.user.AppUserVO;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;


public final class AutoMapper {

    private AutoMapper() {}
    public static final AutoMapper INSTANCE = new AutoMapper();

    // 1) 增加工厂表
    private final Map<Class<?>, java.util.function.Supplier<?>> factories = new ConcurrentHashMap<>();

    // 2) 暴露注册工厂的方法
    public <T> void registerFactory(Class<T> targetClass, java.util.function.Supplier<T> factory) {
        factories.put(targetClass, factory);
    }

    /** 类型对专用转换器注册表（优先级最高） */
    private final Map<Key, Function<Object, Object>> converters = new ConcurrentHashMap<>();

    /** 字段重命名注册表： source->target 字段名映射 */
    private final Map<Key, Map<String,String>> renames = new ConcurrentHashMap<>();



    /** 是否忽略 null 值（默认 true） */
    private volatile boolean ignoreNulls = true;

    public void setIgnoreNulls(boolean ignoreNulls) { this.ignoreNulls = ignoreNulls; }

    /** 注册类型对的专用转换器（高性能热点可用它覆盖默认反射拷贝） */
    public <S,T> void registerConverter(Class<S> src, Class<T> dst, Function<S,T> fn) {
        converters.put(Key.of(src, dst), (Function<Object,Object>) fn);
    }

    /** 注册字段重命名：source.name -> targetName */
    public <S,T> void registerRename(Class<S> src, Class<T> dst, Map<String,String> nameMap) {
        renames.computeIfAbsent(Key.of(src, dst), k -> new HashMap<>()).putAll(nameMap);
    }

    // 3) map(...) 里创建目标对象时，先走工厂，再走无参构造：
    @SuppressWarnings("unchecked")
    public <S,T> T map(S source, Class<T> targetClass) {
        if (source == null) return null;
        final Class<?> srcClz = source.getClass();
        final Key key = Key.of(srcClz, targetClass);

        // 专用转换器优先
        Function<Object,Object> sp = converters.get(key);
        if (sp != null) return (T) sp.apply(source);

        try {
            T target;
            java.util.function.Supplier<?> f = factories.get(targetClass);
            if (f != null) {
                target = (T) f.get();              // 用工厂创建
            } else {
                target = targetClass.getDeclaredConstructor().newInstance(); // 退回无参构造
            }
            copyByBeanProps(source, target, renames.get(key), ignoreNulls);
            return target;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("AutoMapper failed: " + srcClz.getName()
                    + " -> " + targetClass.getName(), e);
        }
    }

    /** 列表转换（保序） */
    public <S,T> List<T> mapList(Collection<S> srcList, Class<T> targetClass) {
        if (srcList == null) return List.of();
        List<T> out = new ArrayList<>(srcList.size());
        for (S s : srcList) out.add(map(s, targetClass));
        return out;
    }

    /* ================= 内部实现（缓存 getters / setters 的 MethodHandle） ================= */

    private record Key(Class<?> s, Class<?> t) {
        static Key of(Class<?> s, Class<?> t) { return new Key(s, t); }
    }

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private static final Map<Class<?>, Map<String, MethodHandle>> GETTERS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Map<String, Setter>> SETTERS = new ConcurrentHashMap<>();

    private static final class Setter {
        final MethodHandle mh; final Class<?> paramType;
        Setter(MethodHandle mh, Class<?> p) { this.mh = mh; this.paramType = p; }
    }

    private static Map<String, MethodHandle> gettersOf(Class<?> clz) {
        return GETTERS.computeIfAbsent(clz, c -> {
            Map<String, MethodHandle> m = new HashMap<>();
            for (Method method : c.getMethods()) {
                if (method.getParameterCount() != 0) continue;
                String name = method.getName();
                Class<?> rt = method.getReturnType();
                String prop = null;
                if (name.startsWith("get") && name.length() > 3 && rt != void.class) {
                    prop = decap(name.substring(3));
                } else if (name.startsWith("is") && name.length() > 2
                        && (rt == boolean.class || rt == Boolean.class)) {
                    prop = decap(name.substring(2));
                }
                if (prop != null) {
                    try {
                        m.put(prop, LOOKUP.unreflect(method));
                    } catch (IllegalAccessException ignored) {}
                }
            }
            return m;
        });
    }

    private static Map<String, Setter> settersOf(Class<?> clz) {
        return SETTERS.computeIfAbsent(clz, c -> {
            Map<String, Setter> m = new HashMap<>();
            for (Method method : c.getMethods()) {
                if (!method.getName().startsWith("set")) continue;
                if (method.getParameterCount() != 1) continue;
                String prop = decap(method.getName().substring(3));
                try {
                    MethodHandle mh = LOOKUP.unreflect(method);
                    m.put(prop, new Setter(mh, method.getParameterTypes()[0]));
                } catch (IllegalAccessException ignored) {}
            }
            return m;
        });
    }

    private static String decap(String s) {
        if (s == null || s.isEmpty()) return s;
        if (s.length() > 1 && Character.isUpperCase(s.charAt(1)) && Character.isUpperCase(s.charAt(0)))
            return s; // URL -> URL
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    private static void copyByBeanProps(Object src, Object dst, Map<String,String> rename, boolean ignoreNulls) {
        Map<String, MethodHandle> gs = gettersOf(src.getClass());
        Map<String, Setter> ss = settersOf(dst.getClass());

        for (Map.Entry<String, MethodHandle> g : gs.entrySet()) {
            String srcProp = g.getKey();
            String targetProp = (rename == null) ? srcProp : rename.getOrDefault(srcProp, srcProp);
            Setter setter = ss.get(targetProp);
            if (setter == null) continue;

            try {
                Object v = g.getValue().invoke(src);
                if (v == null && ignoreNulls) continue;

                // 类型兼容再赋
                if (v == null || setter.paramType.isInstance(v) || isPrimitiveBoxMatch(setter.paramType, v.getClass())) {
                    setter.mh.invoke(dst, v);
                } else {
                    // 简单 String <-> number/enum 兜底（常见场景）
                    Object converted = trySimpleConvert(v, setter.paramType);
                    if (converted != UNCONVERTABLE) setter.mh.invoke(dst, converted);
                }
            } catch (Throwable ignored) {}
        }
    }

    private static final Object UNCONVERTABLE = new Object();

    private static Object trySimpleConvert(Object v, Class<?> want) {
        if (v == null) return null;
        if (want.isEnum() && v instanceof String s) {
            try { return Enum.valueOf((Class<Enum>) want.asSubclass(Enum.class), s); } catch (Exception ignored) {}
        }
        if ((want == String.class)) return String.valueOf(v);
        if ((want == Long.class || want == long.class) && v instanceof String s) { try { return Long.valueOf(s); } catch (Exception ignored) {} }
        if ((want == Integer.class || want == int.class) && v instanceof String s) { try { return Integer.valueOf(s); } catch (Exception ignored) {} }
        if ((want == Double.class || want == double.class) && v instanceof String s) { try { return Double.valueOf(s); } catch (Exception ignored) {} }
        return UNCONVERTABLE;
    }

    private static boolean isPrimitiveBoxMatch(Class<?> want, Class<?> have) {
        if (!want.isPrimitive()) return false;
        return (want == int.class    && have == Integer.class)
            || (want == long.class   && have == Long.class)
            || (want == double.class && have == Double.class)
            || (want == float.class  && have == Float.class)
            || (want == boolean.class&& have == Boolean.class)
            || (want == short.class  && have == Short.class)
            || (want == byte.class   && have == Byte.class)
            || (want == char.class   && have == Character.class);
    }


    public static void main(String[] args) {

        // 如果 AppUserVO 是 @Builder，没有无参构造：
        AutoMapper.INSTANCE.registerFactory(AppUserVO.class, () -> AppUserVO.builder().build());

        // 1) 直接用：同名同类型字段自动映射
        SysUser user = new SysUser();
        user.setId(1L);
        user.setRole(SysUser.Role.OPERATOR);
        AppUserVO dto = AutoMapper.INSTANCE.map(user, AppUserVO.class);
        System.out.println(dto.getId());

        // 2) 列表：
       // List<OrderDTO> list = AutoMapper.INSTANCE.mapList(entities, OrderDTO.class);

        // 3) 开启/关闭“忽略 null”
      //  AutoMapper.INSTANCE.setIgnoreNulls(true); // 默认就是 true

        // 4) 某一对类型字段重命名：source.userName -> target.name
       // AutoMapper.INSTANCE.registerRename(UserEntity.class, UserDTO.class, Map.of("userName", "name"));

        // 5) 给热点类型对注册“专用转换器”（更快/更复杂的逻辑）
       /* AutoMapper.INSTANCE.registerConverter(UserEntity.class, UserDTO.class, e -> {
            UserDTO t = new UserDTO();
            t.setId(e.getId());
            t.setName(e.getUserName());
            t.setDepartment(e.getDepartment()); // 同名同类型也可交给默认逻辑
            return t;
        });*/

    }

}
