package edu.neu.testing;

import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class DowntimeTools {
	static Type listOfLong = new TypeToken<List<Long>>(){}.getType();
	static Type listOfDouble = new TypeToken<List<Double>>(){}.getType();
	static Type listOfEpoch = new TypeToken<List<Epoch>>(){}.getType();
	
	public static String listLongToJson(List<Long> l) {
		return new Gson().toJson(l, listOfLong);
	}
	
	public static String listDoubleToJson(List<Double> l) {
		return new Gson().toJson(l, listOfDouble);
	}
	
	public static String listEpochToJson(List<Epoch> l) {
		return new Gson().toJson(l, listOfEpoch);
	}
	
	public static List<Long> jsonToListLong(String s) {
		return new Gson().fromJson(s, listOfLong);
	}
	
	public static List<Double> jsonToListDouble(String s) {
		return new Gson().fromJson(s, listOfDouble);
	}
	
	public static List<Epoch> jsonToListEpoch(String s) {
		return new Gson().fromJson(s, listOfEpoch);
	}
}
