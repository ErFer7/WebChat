package com.ufsc.webchat.utils;

import java.util.HashMap;
import java.util.Map;

import javax.management.openmbean.KeyAlreadyExistsException;

public class OptionalKeyPairMap<K, T, V> {

	private final HashMap<K, Integer> mapA;
	private final HashMap<T, Integer> mapB;
	private final HashMap<Integer, V> finalMap;
	private int index;

	public OptionalKeyPairMap() {
		this.mapA = new HashMap<>();
		this.mapB = new HashMap<>();
		this.finalMap = new HashMap<>();
		this.index = 0;
	}

	public void put(K keyA, T keyB, V value) throws KeyAlreadyExistsException {
		if (this.mapA.get(keyA) != null || this.mapB.get(keyB) != null) {
			throw new KeyAlreadyExistsException("Key already exists");
		}

		this.mapA.put(keyA, this.index);
		this.mapB.put(keyB, this.index);

		this.finalMap.put(this.index, value);

		this.index++;
	}

	public V getByFirstKey(K keyA) {
		return this.finalMap.get(this.mapA.get(keyA));
	}

	public V getBySecondKey(T keyB) {
		return this.finalMap.get(this.mapB.get(keyB));
	}

	public void removeByFirstKey(K keyA) {
		this.remove(keyA, (HashMap<Object, Integer>) this.mapA, (HashMap<Object, Integer>) this.mapB);
	}

	public void removeBySencondKey(T keyB) {
		this.remove(keyB, (HashMap<Object, Integer>) this.mapB, (HashMap<Object, Integer>) this.mapA);
	}

	private void remove(Object key, HashMap<Object, Integer> firstMap, HashMap<Object, Integer> secondMap) {
		Integer index = firstMap.get(key);

		this.finalMap.remove(index);
		firstMap.remove(key);

		for(Map.Entry<Object, Integer> entry : secondMap.entrySet()) {
			if (entry.getValue().equals(index)) {
				secondMap.remove(entry.getKey());
				break;
			}
		}
	}
}
