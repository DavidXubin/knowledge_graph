package com.bkjk.kgraph.utils;

import java.security.MessageDigest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;


public class Toolkit {

    private static final char[] HEX = {'0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    private static Random rand = new Random();

    public static boolean isDouble(String str){
        if (str == null || str.length() == 0) {
            return false;
        }

        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    public static boolean isInteger(String str){
        if (str == null || str.length() == 0) {
            return false;
        }

        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private static String getFormattedText(byte[] bytes) {
        int len = bytes.length;
        StringBuilder buf = new StringBuilder(len * 2);
        // 把密文转换成十六进制的字符串形式
        for (int j = 0; j < len; j++) {
            buf.append(HEX[(bytes[j] >> 4) & 0x0f]);
            buf.append(HEX[bytes[j] & 0x0f]);
        }
        return buf.toString();
    }

    public static String encode(String str) {
        if (str == null) {
            return null;
        }
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
            messageDigest.update(str.getBytes());
            return getFormattedText(messageDigest.digest());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> void swap(T[] a, int i, int j){
        T temp = a[i];
        a[i] = a[j];
        a[j] = temp;
    }

    public static <T> void shuffle(T[] arr) {
        int length = arr.length;
        for (int i = length; i > 0; i--){
            int randInd = rand.nextInt(i);
            swap(arr, randInd, i - 1);
        }
    }

    public static String shuffle(String s) {
        Byte[] dest = new Byte[s.length()];
        byte[] src = s.getBytes();
        for (int i = 0; i < src.length; i++) {
            dest[i] = src[i];
        }

        shuffle(dest);
        return dest.toString();
    }

    public static Date convertDate(String inputDate) throws ParseException {
        // TODO Handle time as part of date or separate

        SimpleDateFormat dateParser = null;
        // Detect the date format and convert it
        if (inputDate.matches("[0-9]*")) {
            for (int i = inputDate.length(); i < 4; i++) {
                inputDate = "0".concat(inputDate);
            }
            // Only numbers Hour / Minute format
            dateParser = new SimpleDateFormat("Hm");
        } else if (inputDate.matches("[0-9]{2}-[A-Za-z]{3}-[0-9]{4}")) {
            // Use dd-MMM-yyyy format
            dateParser = new SimpleDateFormat("dd-MMM-yyyy");
        } else if (inputDate.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
            // Use yyyy-mm-dd format
            dateParser = new SimpleDateFormat("yyyy-mm-dd");
        } else if (inputDate.matches("[0-9]{2}/[0-9]{2}/[0-9]{2}")) {
            // Use dd/mm/yy format
            dateParser = new SimpleDateFormat("dd/mm/yy");
        } else if (inputDate.matches("[0-9]+\\.[0-9]{2}\\.[0-9]{4}")) {
            // dd.mm.yyyy
            dateParser = new SimpleDateFormat("dd.mm.yyyy");
        } else {
            // Default use MM/dd/yy
            dateParser = new SimpleDateFormat("MM/dd/yy hh:mm");
        }

        return dateParser.parse(inputDate);
    }

    public static Object convertPropertyValue(String value, Class<?> dataType) throws ParseException {
        Object convertedValue = null;

        if (dataType == Integer.class) {
            convertedValue = new Integer(value);
        } else if (dataType == Float.class) {
            convertedValue = new Float(value);
        } else if (dataType == Double.class) {
            convertedValue = new Double(value);
        } else if (dataType == Date.class) {
            convertedValue = convertDate(value);
        } else {
            convertedValue = value;
        }
        return convertedValue;
    }


    public static Date addAndSubtractDays(Date dateTime, int days){

        return new Date(dateTime.getTime() + days * 24 * 60 * 60 * 1000L);
    }


    public static String addAndSubtractDays(String dateTime, int days) throws ParseException {

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

        Date day = df.parse(dateTime);

        return df.format(new Date(day.getTime() + days * 24 * 60 * 60 * 1000L));
    }

}
