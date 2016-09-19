package com.wuyz.removeannotation;

import org.mozilla.intl.chardet.CharsetDetector;

import java.io.*;

public class Main {

    private static final String PATTERN = "(//[^\n]*)|(/\\*(.|\\s)*?\\*/)";
    private static final String PATTERN2 = "(?<!:)//.*|/\\*(\\s|.)*?\\*/";

    public static void main(String[] args) {
        if (args == null || args.length != 1) {
            System.err.println("Error! usage: java RemoveAnnotation dir");
            return;
        }
        File dir = new File(args[0]);
        processDir(dir);
        System.out.println("done!");
    }

    private static void processDir(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory())
            return;
        File[] files = dir.listFiles();
        if (files == null)
            return;
        for (File f : files) {
            String name = f.getName().toLowerCase();
            if (f.isDirectory()) {
                processDir(f);
            } else if (name.endsWith(".java")
                    || name.endsWith(".aidl")
                    || name.endsWith(".cpp")
                    || name.endsWith(".c")) {
                FileInputStream inputStream = null;
                FileOutputStream outputStream = null;
                String content = null;
                String charset = null;
                try {
                    charset = CharsetDetector.getCharset(f);
                    if (charset == null) {
                        System.err.println("can't recognize charset: " + f.getPath());
                        return;
                    }
                    inputStream = new FileInputStream(f);
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(1024);
                    int n;
                    byte[] buffer = new byte[1024];
                    while ((n = inputStream.read(buffer)) != -1) {
                        byteArrayOutputStream.write(buffer, 0, n);
                    }
                    inputStream.close();
                    byteArrayOutputStream.close();
                    content = byteArrayOutputStream.toString(charset);
//                    System.out.println("before: " + f.getPath() + ":\n" + content);
                    String result = content.replaceAll(PATTERN, "");
//                    System.out.println(f.getPath() + ":\n" + content);
                    if (!result.equals(content)) {
                        outputStream = new FileOutputStream(f);
                        outputStream.write(result.getBytes(charset));
                        System.out.println("modified: " + f.getPath());
                        outputStream.close();
                    }
                } catch (StackOverflowError e) {
                    if (content != null) {
                        boolean found = false;
                        while (true) {
                            int start = content.indexOf("/*");
                            if (start == -1)
                                break;
                            int end = content.indexOf("*/", start + 2);
                            if (end > start) {
                                found = true;
                                content = content.substring(0, start) + content.substring(end + 2);
                            }
                        }
                        if (found) {
                            try {
                                outputStream = new FileOutputStream(f);
                                outputStream.write(content.getBytes(charset));
                                System.out.println("StackOverflowError modified: " + f.getPath());
                                outputStream.close();
                            } catch (Exception e1) {
                                e1.printStackTrace();
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (outputStream != null) {
                        try {
                            outputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}
