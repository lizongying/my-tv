package com.lizongying.mytv.jce;

import com.qq.taf.jce.JceInputStream;
import com.qq.taf.jce.JceOutputStream;
import com.qq.taf.jce.JceStruct;

import java.lang.reflect.Field;

public class a {

    /* renamed from: a  reason: collision with root package name */
    private static final String f33240a = "Net_Protocol";

    private a() {
    }

    private static JceStruct a(JceStruct jceStruct, ClassLoader classLoader) {
        Class<?> cls;
        if (jceStruct == null) {
            return null;
        }
        String name = jceStruct.getClass().getName();
        String str = name.substring(0, name.length() - 7) + "Response";
        try {
            if (classLoader == null) {
                cls = Class.forName(str);
            } else {
                cls = Class.forName(str, true, classLoader);
            }
            return (JceStruct) cls.newInstance();
        } catch (ClassNotFoundException unused) {
            return null;
        } catch (IllegalAccessException unused) {
            return null;
        } catch (InstantiationException unused) {
            return null;
        }
    }

    private static JceStruct b(String str, ClassLoader classLoader) {
        Class<?> cls;
        if (str == null) {
            return null;
        }
        String str2 = str + "Response";
        try {
            if (classLoader == null) {
                cls = Class.forName(str2);
            } else {
                cls = Class.forName(str2, true, classLoader);
            }
            return (JceStruct) cls.newInstance();
        } catch (ClassNotFoundException unused) {
            return null;
        } catch (IllegalAccessException unused) {
            return null;
        } catch (InstantiationException unused) {
            return null;
        }
    }

    public static int c(Object obj) {
        Field[] declaredFields;
        int i2 = 0;
        if (obj == null) {
            return 0;
        }
        for (Field field : obj.getClass().getDeclaredFields()) {
            if (field != null) {
                field.setAccessible(true);
                if (!field.getName().equals("errCode")) {
                    continue;
                } else if (!field.getType().getName().equals(Integer.class.getName()) && !field.getType().getName().equals("int")) {
                    if (field.getType().getName().equals(Long.class.getName()) || field.getType().getName().equals("long")) {
                        try {
                            return (int) field.getLong(obj);
                        } catch (IllegalAccessException e2) {
                            e2.printStackTrace();
                        } catch (IllegalArgumentException e3) {
                            e3.printStackTrace();
                        }
                    }
                } else {
                    try {
                        return field.getInt(obj);
                    } catch (IllegalAccessException e4) {
                        e4.printStackTrace();
                    } catch (IllegalArgumentException e5) {
                        e5.printStackTrace();
                    }
                }
            }
        }
        return i2;
    }

    public static byte[] d(JceStruct jceStruct) {
        if (jceStruct == null) {
            return null;
        }
        JceOutputStream jceOutputStream = new JceOutputStream();
        jceOutputStream.setServerEncoding("utf-8");
        jceStruct.writeTo(jceOutputStream);
        return jceOutputStream.toByteArray();
    }

    public static JceStruct e(JceStruct jceStruct, byte[] bArr, ClassLoader classLoader) {
        JceStruct a2;
        if (jceStruct != null && bArr != null && (a2 = a(jceStruct, classLoader)) != null) {
            try {
                JceInputStream jceInputStream = new JceInputStream(bArr);
                jceInputStream.setServerEncoding("utf-8");
                a2.readFrom(jceInputStream);
                return a2;
            } catch (Exception unused) {
            }
        }
        return null;
    }

    public static JceStruct f(String str, byte[] bArr, ClassLoader classLoader) {
        if (str != null && bArr != null) {
            JceStruct b2 = b(str, classLoader);
            if (b2 != null) {
                try {
                    JceInputStream jceInputStream = new JceInputStream(bArr);
                    jceInputStream.setServerEncoding("utf-8");
                    b2.readFrom(jceInputStream);
                    return b2;
                } catch (Exception e2) {
                    return null;
                }
            }
        }
        return null;
    }

    public static <T extends JceStruct> T g(byte[] bArr, Class<T> cls) {
        T t;
        if (bArr != null && bArr.length >= 4) {
            try {
                t = cls.newInstance();
            } catch (IllegalAccessException unused) {
                t = null;
            } catch (InstantiationException unused) {
                t = null;
            }
            try {
                JceInputStream jceInputStream = new JceInputStream(bArr);
                jceInputStream.setServerEncoding("utf-8");
                if (t != null) {
                    t.readFrom(jceInputStream);
                }
                return t;
            } catch (Exception unused2) {
            }
        }
        return null;
    }
}
