package bearmaps.proj2c;

import java.util.*;

public class MyTrieSet implements TrieSet61B {

    public Node root;

    private static class Node {
        private char character;
        private boolean isKey;
        private HashMap <Character, Node> map;

        public Node() {
            isKey = false;
            map = new HashMap<>();
        }

        public Node(char c) {
            character = c;
            isKey = false;
            map = new HashMap<>();
        }
    }

    public MyTrieSet() {
        root = new Node();
    }

    /** Clears all items out of Trie */
    public void clear() {
        root = new Node();
    }

    /** Returns true if the Trie contains KEY, false otherwise */
    public boolean contains(String key) {

        Node current = root;
        for(int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if(!current.map.containsKey(c)){
                return false;
            }
            current = current.map.get(c);
        }

        return current.isKey;
    }

    /** Inserts string KEY into Trie */
    public void add(String key) {
        Node current = root;

        for(int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if(!current.map.containsKey(c)) {
                current.map.put(c, new Node(c));
            }
            current = current.map.get(c);
        }
        current.isKey = true;
    }

    /** Returns a list of all words that start with PREFIX */
    public List<String> keysWithPrefix(String prefix) {
        List<String> list = new ArrayList<>();
        Node current = root;

        for(int i  = 0; i < prefix.length(); i++) {
            char c = prefix.charAt(i);
            if(!current.map.containsKey(c)){
                return list;
            }
            current = current.map.get(c);
        }

        if(current.isKey){
            list.add(prefix);
        }

        Set<Character> keys = current.map.keySet();

        for(char c : keys) {
            keysWithPrefixHelper(current.map.get(c), list, new StringBuilder(prefix));
        }

        return list;
    }

    private void keysWithPrefixHelper(Node n, List<String> list, StringBuilder prefix) {

        if(n.isKey) {
            list.add(prefix.toString().concat(((Character) n.character).toString()));
        }

        Set<Character> keys = n.map.keySet();
        for(char c : keys) {
            keysWithPrefixHelper(n.map.get(c), list, new StringBuilder(prefix).append(n.character));
        }

    }

    /** Returns the longest prefix of KEY that exists in the Trie
     * Not required for Lab 9. If you don't implement this, throw an
     * UnsupportedOperationException.
     */
    public String longestPrefixOf(String key) {
        throw new UnsupportedOperationException();
    }

    public static void main(String[] args) {

        MyTrieSet a = new MyTrieSet();
        a.add("apple");
        boolean t = a.contains("apple");
        boolean f =a.contains("pie");

    }
}
