package bearmaps.proj2ab;

import java.util.*;

public class ArrayHeapMinPQ<T> implements ExtrinsicMinPQ<T> {

    int size;   // number of items in pq
    ArrayList<Node> pq;
    HashSet<T> listItems;
    HashMap<T, Integer> itemIndexPair;  // item-index pair, given item, give back index to pq.
    //private Comparator<T> comparator;

    public ArrayHeapMinPQ(){
        this(1);
    }

    public ArrayHeapMinPQ(int capacity){
        size = 0;
        pq = new ArrayList<>(capacity + 1);
        pq.add(0, null); // set first item to null
        listItems = new HashSet<>();
        itemIndexPair = new HashMap<>();
    }

    public class Node {
        T item;
        double priority;

        public Node(T item, double priority) {
            this.item = item;
            this.priority = priority;
        }

        private T getItem() {return item;}
        private double getPriority() {return priority;}
        private void changePriority(double newPriority) {priority = newPriority;}
    }

    /* Adds an item with the given priority value. Throws an
     * IllegalArgumentException if item is already present.
     * You may assume that item is never null. */
    public void add(T item, double priority) {
        if(contains(item))
            throw new IllegalArgumentException();

        listItems.add(item);
        Node addedItem = new Node(item, priority);
        size += 1;
        pq.add(size, addedItem);
        itemIndexPair.put(item, size); // go back to this
        swim(size);
    }

    private void swim(int index){

        while(index > 1 && greater(index/2, index)){
            exchange(index, index/2);
            index = index/2;
        }
    }

    private boolean greater(int i, int j) {

//        if(comparator == null) {
//            return ((Comparable<Double>) pq.get(i).priority).compareTo(pq.get(j).priority) > 0;
//        } else {
//            return comparator.compare(pq.get(i).priority, pq.get(j).priority) > 0;
//        }
        return ((Comparable<Double>) pq.get(i).getPriority()).compareTo(pq.get(j).getPriority()) > 0;
    }

    private void exchange(int i, int j){

        // switching indices of item in itemIndexPair
        itemIndexPair.replace(pq.get(i).item, j);
        itemIndexPair.replace(pq.get(j).item, i);

        Node temp = pq.get(i);
        pq.set(i, pq.get(j));
        pq.set(j, temp);
    }

    /* Returns true if the PQ contains the given item. */
    public boolean contains(T item) { return listItems.contains(item);}
    /* Returns the minimum item. Throws NoSuchElementException if the PQ is empty. */
    public T getSmallest() {
        if (isEmpty())
            throw new IllegalArgumentException();

        return pq.get(1).getItem();
    }

    private boolean isEmpty() {
        if (size() == 0)
            return true;

        return false;
    }

    private void sink(int index) {

        while(2*index <= size()){
            int j = 2*index;
            if (j < size() && greater(j, j+ 1))
                j += 1;
            if(!greater(index, j))
                break;
            exchange(index, j);
            index = j;
        }
    }

    /* Removes and returns the minimum item. Throws NoSuchElementException if the PQ is empty. */
    public T removeSmallest() {
        T smallest = getSmallest();
        itemIndexPair.remove(pq.get(1).item); // remove smallest item-index pair

        Node min = pq.get(1);
        exchange(1, size);
        size -= 1;
        sink(1);
        //pq.set(size+1) = null;

        return smallest;
    }

    /* Returns the number of items in the PQ. */
    public int size() { return size; }

    /* Changes the priority of the given item. Throws NoSuchElementException if the item
     * doesn't exist. */
    public void changePriority(T item, double priority) {

        if(!listItems.contains(item))
            throw new NoSuchElementException();;

        int currentLocationItem = itemIndexPair.get(item);
        pq.get(currentLocationItem).changePriority(priority);

        // change priority needed to be changed is at root.
        if (pq.get((currentLocationItem/2)) == null) {
            sink(currentLocationItem);
            return;
        }

        // check whether to sink or swim
        double parentPriority = pq.get(currentLocationItem/2).getPriority();
        if (parentPriority > priority)
            swim(currentLocationItem);
        else
            sink(currentLocationItem);

    }

    public ArrayList<T> itemsInPQ() {
        //T[] localItem = (T[]) new Object[size()];
        ArrayList<T> localItem = new ArrayList<>();

        for(int i = 1; i < size() + 1; i++)
            localItem.add(pq.get(i).item);

        return localItem;
    }

    public static void main(String[] args) {

    }
}
