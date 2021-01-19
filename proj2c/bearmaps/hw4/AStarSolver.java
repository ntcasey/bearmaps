package bearmaps.hw4;

import bearmaps.proj2ab.DoubleMapPQ;
import bearmaps.proj2ab.ArrayHeapMinPQ;
import edu.princeton.cs.algs4.Stopwatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class AStarSolver<Vertex> implements ShortestPathsSolver<Vertex>
{
    public SolverOutcome outcome;
    public double solutionWeight;
    public LinkedList<Vertex> solution;
    public double timeSpent;
    public int numDequeOperation;

    public AStarSolver(AStarGraph<Vertex> input, Vertex start, Vertex end, double timeout) {
        Stopwatch sw = new Stopwatch();
        numDequeOperation = 0;

        LinkedList<Vertex> path = new LinkedList<>();
        ArrayHeapMinPQ<Vertex> pq = new ArrayHeapMinPQ<>();
        pq.add(start, input.estimatedDistanceToGoal(start, end));

        Vertex smallest;
        ArrayList<WeightedEdge<Vertex>> edge;
        HashMap<Vertex, Double> distTo = new HashMap<>(); // <to, distanceFromStartToCurrentVertex>
        HashMap<Vertex, Vertex> edgeTo = new HashMap<>(); // <to, from>
        distTo.put(start, 0.0);

        while(pq.size() > 0 && !pq.getSmallest().equals(end)) {
            smallest = pq.removeSmallest();
            numDequeOperation += 1;
            edge = (ArrayList<WeightedEdge<Vertex>>) input.neighbors(smallest);

            // relax edge
            for (WeightedEdge<Vertex> i: edge) {

                Vertex p = i.from();
                Vertex q = i.to();
                double w = i.weight();

                if(!distTo.containsKey(q) || distTo.get(p) + w < distTo.get(q)){
                    distTo.put(q, distTo.get(p) + w);

                    if(pq.contains(q)) {
                        edgeTo.replace(q, p);
                        pq.changePriority(q, distTo.get(q) + input.estimatedDistanceToGoal(q, end));
                    } else {
                        edgeTo.put(q, p);
                        pq.add(q, distTo.get(q) + input.estimatedDistanceToGoal(q, end));
                    }
                }
            }
        }
        timeSpent = sw.elapsedTime();

        if (timeSpent > timeout || pq.size() == 0) {
            outcome = (pq.size() == 0) ? SolverOutcome.UNSOLVABLE : SolverOutcome.TIMEOUT;
            solutionWeight = 0;
            solution = new LinkedList<>();
            return;
        }

        Vertex v = edgeTo.get(end);
        path.addFirst(end);
        while(v != null) {
            path.addFirst(v);
            v = edgeTo.get(v);
        }
        outcome = SolverOutcome.SOLVED;
        solutionWeight = distTo.get(end);
        solution = path;
    }

    @Override
    public SolverOutcome outcome() {
        return outcome;
    }

//    /*** A list of vertices corresponding to a solution.
//     * Should be empty if result was TIMEOUT or UNSOLVABLE. ***/
//    @Override
//    public List<Vertex> solution() {
//        return solution;
//    }
//
//    /*** The total weight of the given solution, taking into account
//     * edge weights. Should be 0 if result was TIMEOUT or UNSOLVABLE. ***/
//    @Override
//    public double solutionWeight() {
//        return solutionWeight;
//    }

    @Override
    public List<Vertex> solution() {
        if (outcome == SolverOutcome.SOLVED) {
            return solution;
        }
        return new ArrayList<>();
    }

    @Override
    public double solutionWeight() {
        if (outcome == SolverOutcome.SOLVED) {
            return solutionWeight;
        }
        return 0;
    }

    /*** The total number of priority queue dequeue operations. ***/
    @Override
    public int numStatesExplored() {
        return numDequeOperation;
    }

    /*** The total time spent in seconds by the constructor. ***/
    @Override
    public double explorationTime() {
        return timeSpent;
    }

    public static void main(String[] args) {

        //ArrayList<Integer> a = new
    }
}