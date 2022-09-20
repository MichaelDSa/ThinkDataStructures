package com.allendowney.thinkdast;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.sql.SQLOutput;
import java.util.*;

/**
 * 
 */
/**
 * @author downey
 *
 */
public class WikiParser {
	
	// the list of paragraphs we should search
	private Elements paragraphs;
	
	// the stack of open delimiters
	// TODO: consider simplifying this by counting parentheses
	private Deque<String> parenthesisStack;
	

	/**
	 * Initializes a WikiParser with a list of Elements.
	 * 
	 * @param paragraphs
	 */
	public WikiParser(Elements paragraphs) {
		this.paragraphs = paragraphs;
		this.parenthesisStack = new ArrayDeque<String>();
	}
	
	/**
	 * Searches the paragraphs for a valid link.
	 * 
	 * Warns if a paragraph ends with unbalanced parentheses.
	 * 
	 * @return
	 */
	public Element findFirstLink() {
		for (Element paragraph: paragraphs) { 
			Element firstLink = findFirstLinkPara(paragraph);
			if (firstLink != null) {
				return firstLink;
			}
			if (!parenthesisStack.isEmpty()) {
				System.err.println("Warning: unbalanced parentheses."); 
	   	 	}
		}
		return null;
	}

	public Queue<String> getAllLinks() {
		Queue<String> queue = new LinkedList<>();
		for (Element paragraph: paragraphs) {
			Element firstLink = findFirstLinkPara(paragraph);
			if (firstLink != null) {
				System.out.println(firstLink.text());
				queue.offer(firstLink.text().trim());
			}
			if (!parenthesisStack.isEmpty()) {
				System.err.println("Warning: unbalanced parentheses.");
			}
		}


		return queue;
	}

	/**
	 * Returns the first valid link in a paragraph, or null.
	 * 
	 * @param root
	 */
	private Element findFirstLinkPara(Node root)  {
		// create an Iterable that traverses the tree
		Iterable<Node> nt = new WikiNodeIterable(root);//iterable DFS
		//-----------------------------------------------------
		//Alternative data structures which can be tried instead of WikiNodeIterable(root).
//		Deque<Node> nt = new ArrayDeque<>(root.childNodes());//this works
//		Deque<Node> nt = new ArrayDeque<>();
//		nt.push((Node)root.childNodes());// does not work. ClassCastException
//		List<Node> nt = new ArrayList<>(root.childNodes());//this works
//		List<Node> nt = new ArrayList<>();
//		nt.add(root);
		// THESE, HOWEVER WILL NOT DO DFS
		//------------------------------------------------------

		// loop through the nodes
		for (Node node: nt) {
//		for (Node node: nt.pop().childNodes()) {//
			// process TextNodes to get parentheses
			if (node instanceof TextNode) {
				processTextNode((TextNode) node);
			}
			// process elements to get find links
			if (node instanceof Element) {
		        Element firstLink = processElement((Element) node);
		        if (firstLink != null) {
					return firstLink;
				}
			}
		}
		return null;
	}

	/**
	 * Returns the element if it is a valid link, null otherwise.
	 * 
	 * 
	 * 
	 * @param elt
	 */
	private Element processElement(Element elt) {
		//System.out.println(elt.tagName());
		if (validLink(elt)) {
			return elt;
		}
		return null;
	}

	/**
	 * Checks whether a link is value.
	 * 
	 * @param elt
	 * @return
	 */
	private boolean validLink(Element elt) {
		// it's no good if it's
		// not a link
		if (!elt.tagName().equals("a")) {
			return false;
		}
		// in italics
		if (isItalic(elt)) {
			return false;
		}
		// in parenthesis
		if (isInParens(elt)) {
			return false;
		}
		// a bookmark
		if (startsWith(elt, "#")) {
			return false;
		}
		// a Wikipedia help page
		if (startsWith(elt, "/wiki/Help:")) {
			return false;
		}
		// TODO: there are a couple of other "rules" we haven't handled
		return true;
	}

	/**
	 * Checks whether a link starts with a given String.
	 * 
	 * @param elt
	 * @param s
	 * @return
	 */
	private boolean startsWith(Element elt, String s) {
		//System.out.println(elt.attr("href"));
		return (elt.attr("href").startsWith(s));
	}

	/**
	 * Checks whether the element is in parentheses (possibly nested).
	 * 
	 * @param elt
	 * @return
	 */
	private boolean isInParens(Element elt) {
		// check whether there are any parentheses on the stack
		return !parenthesisStack.isEmpty();
	}

	/**
	 * Checks whether the element is in italics.
	 * 
	 * (Either a "i" or "em" tag)
	 * 
	 * @param start
	 * @return
	 */
	private boolean isItalic(Element start) {
		// follow the parent chain until we get to null
		for (Element elt=start; elt != null; elt = elt.parent()) {
			if (elt.tagName().equals("i") || elt.tagName().equals("em")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Processes a text node, splitting it up and checking parentheses.
	 * 
	 * @param node
	 */
	private void processTextNode(TextNode node) {
		StringTokenizer st = new StringTokenizer(node.text(), " ()", true);
		while (st.hasMoreTokens()) {
		     String token = st.nextToken();
		     // System.out.print(token);
		     if (token.equals("(")) {
		    	 parenthesisStack.push(token);
		     }
		     if (token.equals(")")) {
		    	 if (parenthesisStack.isEmpty()) {
		    		 System.err.println("Warning: unbalanced parentheses."); 
		    	 }
		    	 parenthesisStack.pop();
		     }
		}
	}
}
