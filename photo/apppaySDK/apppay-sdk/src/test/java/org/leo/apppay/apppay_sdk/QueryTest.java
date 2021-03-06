package org.leo.apppay.apppay_sdk;

import java.util.HashMap;
import java.util.Map;

import org.leo.apppay.apppay_sdk.utils.HttpClient;
import org.leo.apppay.apppay_sdk.utils.MD5;
import org.leo.apppay.apppay_sdk.utils.SignUtils;

/**
 * 
 * 订单查询
 *
 */
public class QueryTest {

	public static void main(String[] args) {
		Map<String, String> map = new HashMap<String, String>();
		String merchantNo = "898875475663024";
		String orderNo = "123456789012372";
		map.put("merchantNo", merchantNo);
		map.put("orderNo", orderNo);
		
		String a = SignUtils.payParamsToString(map);
		a = a+"9d101c97133837e13dde2d32a5054abb";
		System.out.println("sign加密前的字符串="+a);
		System.out.println("密钥=9d101c97133837e13dde2d32a5054abb");
		String sign = MD5.md5Str(a).toUpperCase();
		System.out.println("MD5加密后的结果="+sign);
		map.put("sign", sign);
		
		String url = "http://wappay.vitongpay.com//apppay/query";
		String response = HttpClient.post(url, map);
		System.out.println("response="+response);

	}

}
