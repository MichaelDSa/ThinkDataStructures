package com.allendowney.thinkdast;

import java.io.IOException;
import java.sql.SQLOutput;
import java.util.*;
import java.util.Map.Entry;

import org.jsoup.select.Elements;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

/**
 * Represents a Redis-backed web search index.
 *
 */
public class JedisIndex {

	private Jedis jedis;

	/**
	 * Constructor.
	 *
	 * @param jedis
	 */
	public JedisIndex(Jedis jedis) {
		this.jedis = jedis;
	}

	/**
	 * Returns the Redis key for a given search term.
	 *
	 * @return Redis key.
	 */
	private String urlSetKey(String term) {
		return "URLSet:" + term;
	}

	/**
	 * Returns the Redis key for a URL's TermCounter.
	 *
	 * @return Redis key.
	 */
	private String termCounterKey(String url) {
		return "TermCounter:" + url;
	}

	/**
	 * Checks whether we have a TermCounter for a given URL.
	 *
	 * @param url
	 * @return
	 */
	public boolean isIndexed(String url) {
		String redisKey = termCounterKey(url);
		return jedis.exists(redisKey);
	}
	
	/**
	 * Adds a URL to the set associated with `term`.
	 * 
	 * @param term
	 * @param tc
	 */
	public void add(String term, TermCounter tc) {
		jedis.sadd(urlSetKey(term), tc.getLabel());
	}

	/**
	 * Looks up a search term and returns a set of URLs.
	 * 
	 * @param term
	 * @return Set of URLs.
	 */
	public Set<String> getURLs(String term) {
        // FILL THIS IN!

		// Not sure how this will work, because Jedis.get() returns a "bulk reply".
		// I don't understand this "bulk reply" concept, but I do understand that
		// jedis.get() will(should!) return multiple values.

		Set<String> trms = jedis.smembers(urlSetKey(term));

		//THIS IS WHAT i TRIED TO DO BEFORE I LEARNED ABOUT .smembers
		//IT RETURNED A "JedisDataException: WRONGTYPE Operation against a key holding the wrong kind of value"
//		Set<String> terms = new TreeSet<>();
//
//		for(String key : urlSetKeys()) {
//			String[] keySplit = key.split(":");
//			String key1 = keySplit[1];
//			if (key1.equals(term)) {
//				terms.add(jedis.get(key));//WRONGTYPE here.
//			}
//		}
		return trms;
	}

    /**
	 * Looks up a term and returns a map from URL to count.
	 * 
	 * @param term
	 * @return Map from URL to count.
	 */
	public Map<String, Integer> getCounts(String term) {
        // FILL THIS IN!

		Map<String, Integer> counter = new HashMap<>();
		Set<String> urlSet = getURLs(term);

		for (String url : urlSet) {
			int count = getCount(url, term);
			counter.put(url, count);
		}
//			String tcKeys = "TermCounter:" + url;
//
//			if (jedis.sismember(tcKeys, term)) {//WRONGTYPE here.
//
//				int tcount = Integer.parseInt(jedis.hget(tcKeys, term));
//
//				if (counter.containsKey(term)) {
//					int tcountSum = counter.get(url) + tcount;
//					counter.put(url, tcountSum);
//				}
//				else {
//					counter.put(tcKeys,tcount);
//				}
//			}
//		}


		return counter;
	}

    /**
	 * Returns the number of times the given term appears at the given URL.
	 * 
	 * @param url
	 * @param term
	 * @return
	 */
	public Integer getCount(String url, String term) {
        // FILL THIS IN!

		int termCount = Integer.parseInt(jedis.hget((termCounterKey(url)),term));

		return termCount;
	}

	/**
	 * Adds a page to the index.
	 *
	 * @param url         URL of the page.
	 * @param paragraphs  Collection of elements that should be indexed.
	 */
	public void indexPage(String url, Elements paragraphs) throws IOException {
		// TODO: FILL THIS IN!

		System.out.println("Indexing: " + url);

		//THIS WAS MY ORGINAL SOLUTION. IT WORKED, BUT TOO MANY LOOPS.
//		Index idx = new Index();
//		idx.indexPage(url,paragraphs);
//
//		Transaction t = jedis.multi();
//
//		for(String term : idx.keySet() ) {
//			for(TermCounter tc : idx.get(term)){
//				t.sadd("URLset:"+term, tc.getLabel());
//				t.hset("TermCounter:"+tc.getLabel(), term, tc.get(term).toString());
//			}
//		}
//		t.exec();

		// This is closer to Downey's solution, but with fewer declarations,
		// and using only one method. Also, the Transaction object does not
		// use a List<Object>, which is something I don't understand at all.
		// Anyway, this works!
		TermCounter tc = new TermCounter(url);
		tc.processElements(paragraphs);

		Transaction t = jedis.multi();

		//if the page has already been index, delete the old hash
		t.del(termCounterKey(url));

		for (String  term : tc.keySet()) {
			t.sadd(urlSetKey(term), url);
			t.hset(termCounterKey(url), term, tc.get(term).toString());
		}
		t.exec();


	}
	/**
	 * Prints the contents of the index.
	 *
	 * Should be used for development and testing, not production.
	 */
	public void printIndex() {
		// loop through the search terms
		for (String term: termSet()) {
			System.out.println(term);

			// for each term, print the pages where it appears
			Set<String> urls = getURLs(term);
			for (String url: urls) {
				Integer count = getCount(url, term);
				System.out.println("    " + url + " " + count);
			}
		}
	}

	/**
	 * Returns the set of terms that have been indexed.
	 *
	 * Should be used for development and testing, not production.
	 *
	 * @return
	 */
	public Set<String> termSet() {
		Set<String> keys = urlSetKeys();
		Set<String> terms = new HashSet<String>();
		for (String key: keys) {
			String[] array = key.split(":");
			if (array.length < 2) {
				terms.add("");
			} else {
				terms.add(array[1]);
			}
		}
		return terms;
	}

	/**
	 * Returns URLSet keys for the terms that have been indexed.
	 *
	 * Should be used for development and testing, not production.
	 *
	 * @return
	 */
	public Set<String> urlSetKeys() {
		return jedis.keys("URLSet:*");
	}

	/**
	 * Returns TermCounter keys for the URLS that have been indexed.
	 *
	 * Should be used for development and testing, not production.
	 *
	 * @return
	 */
	public Set<String> termCounterKeys() {
		return jedis.keys("TermCounter:*");
	}

	/**
	 * Deletes all URLSet objects from the database.
	 *
	 * Should be used for development and testing, not production.
	 *
	 * @return
	 */
	public void deleteURLSets() {
		Set<String> keys = urlSetKeys();
		Transaction t = jedis.multi();
		for (String key: keys) {
			t.del(key);
		}
		t.exec();
	}

	/**
	 * Deletes all URLSet objects from the database.
	 *
	 * Should be used for development and testing, not production.
	 *
	 * @return
	 */
	public void deleteTermCounters() {
		Set<String> keys = termCounterKeys();
		Transaction t = jedis.multi();
		for (String key: keys) {
			t.del(key);
		}
		t.exec();
	}

	/**
	 * Deletes all keys from the database.
	 *
	 * Should be used for development and testing, not production.
	 *
	 * @return
	 */
	public void deleteAllKeys() {
		Set<String> keys = jedis.keys("*");
		Transaction t = jedis.multi();
		for (String key: keys) {
			t.del(key);
		}
		t.exec();
	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		Jedis jedis = JedisMaker.make();
		JedisIndex index = new JedisIndex(jedis);

//		index.deleteTermCounters();
//		index.deleteURLSets();
//		index.deleteAllKeys();
		loadIndex(index);

		Map<String, Integer> map = index.getCounts("the");
		for (Entry<String, Integer> entry: map.entrySet()) {
			System.out.println(entry);
		}
	}

	/**
	 * Stores two pages in the index for testing purposes.
	 *
	 * @return
	 * @throws IOException
	 */
	private static void loadIndex(JedisIndex index) throws IOException {
		WikiFetcher wf = new WikiFetcher();

		String url = "https://en.wikipedia.org/wiki/Java_(programming_language)";
		Elements paragraphs = wf.readWikipedia(url);
		index.indexPage(url, paragraphs);

		url = "https://en.wikipedia.org/wiki/Programming_language";
		paragraphs = wf.readWikipedia(url);
		index.indexPage(url, paragraphs);
	}
}
