public class TasksLinkedList {
    private Node dummy;
    private Node curr;
    private Node origin;
    private Node nextNode;
    public TasksLinkedList() {
        curr = new Node(0);
        this.dummy = new Node(100);
        this.dummy.setNext_node(curr);
    }
    public void addNode(int seq){
        nextNode = new Node(seq);
        curr.setNext_node(nextNode);
        curr = nextNode;
    }

    public void moveForward(){
        if(origin.hasNext()){
            origin = origin.getNext_node();
        }
    }

    public Node getDummy(){
        return dummy;
    }


    class Node{
        private Node next_node;
        private int seqNum;
        public Node(int seqNum) {
            this.seqNum = seqNum;
        }
        public void setNext_node(Node next_node) {
            this.next_node = next_node;
        }

        public Node getNext_node() {
            return next_node;
        }

        public boolean hasNext(){
            return (next_node!=null);
        }

    }
}
