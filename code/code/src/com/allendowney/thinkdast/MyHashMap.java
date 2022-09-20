/**
 *
 */
package com.allendowney.thinkdast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implementation of a HashMap using a collection of MyLinearMap and
 * resizing when there are too many entries.
 *
 * @author downey
 * @param <K>
 * @param <V>
 *
 */
public class MyHashMap<K, V> extends MyBetterMap<K, V> implements Map<K, V> {

	// average number of entries per map before we rehash
	protected static final double FACTOR = 1.0;

	@Override
	public V put(K key, V value) {
		V oldValue = super.put(key, value);

		//System.out.println("Put " + key + " in " + map + " size now " + map.size());

		// check if the number of elements per map exceeds the threshold
		if (size() > maps.size() * FACTOR) {
			rehash();
		}
		return oldValue;
	}

	/**
	 * Doubles the number of maps and rehashes the existing entries.
	 */
	/**
	 *
	 */
	protected void rehash() {
		// TODO: FILL THIS IN!
		// TWO SOLUTIONS:

		// SOLUTION 1 (my solution):
		MyBetterMap<K,V> newMap = new MyBetterMap<>();

		int newSize = maps.size() * 2;
		newMap.makeMaps(newSize);

		for(MyLinearMap<K,V> i : maps) {
			for(Map.Entry<K,V> j : i.getEntries()) {
				newMap.put(j.getKey(), j.getValue());
			}
		}
		/* I was wondering why, when I omit the assignment line below this program works properly,
		* and why MyHashTest passes all tests. I realized that because MyLinearMap and MyBetterMap
		* are based on ArrayLists, They will grow indefinitely. Therefore, the tests should pass even when I
		* comment out this whole method, which it does, and the program still works. I can print an
		* iteration from 0 to 10 without the rehashing method. maps.size() remains at 2. */
		maps = newMap.maps; // without this line, maps.size() stays at 2.

		//SOLUTION 2 (Downey's solution):
//		List<MyLinearMap<K,V>> oldMaps = maps;
//
//		int newSize = maps.size() *2;
//		makeMaps(newSize);
//
//		for(MyLinearMap<K,V> i : oldMaps){
//			for(Map.Entry<K,V> j : i.getEntries()){
//				put(j.getKey(), j.getValue());
//			}
//		}
		//MY TESTS
//		System.out.println("oldMaps size: " + oldMaps.size());
//		System.out.println("newMap size: " + newMap.size());
//		System.out.println("maps size: " + maps.size());
//		int count = 0;
//		System.out.print("[");
//		for(MyLinearMap<K,V> i : maps) {
//			for(Map.Entry<K,V> j : i.getEntries()){
//				System.out.print(count++ + ", ");
//			}
//		}
//		System.out.print("]\n");
//		System.out.println("----------------------------");
//		maps = newMap.maps;

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Map<String, Integer> map = new MyHashMap<String, Integer>();
		for (int i=0; i<10; i++) {
			map.put(new Integer(i).toString(), i);
		}
		Integer value = map.get("3");
		System.out.println(value);
		System.out.println();

		for(String i : map.keySet()) {
			System.out.print(map.get(i) + ", ");
		}
	}
}
