package com.msvastudios.trick_builder.node_editor.node;

import android.content.Context;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;

import com.msvastudios.trick_builder.R;
import com.msvastudios.trick_builder.node_editor.line.LinePoint;
import com.msvastudios.trick_builder.node_editor.line.LinesView;
import com.msvastudios.trick_builder.node_editor.node.item.ConnectorCallback;
import com.msvastudios.trick_builder.node_editor.node.item.connectors.NodeConnectorItem;
import com.msvastudios.trick_builder.node_editor.node.item.connectors.NodeInput;
import com.msvastudios.trick_builder.node_editor.node.item.NodeItem;
import com.msvastudios.trick_builder.node_editor.node.item.NodeNav;
import com.msvastudios.trick_builder.node_editor.node.item.connectors.NodeOutput;
import com.msvastudios.trick_builder.node_editor.node.item.Type;
import com.msvastudios.trick_builder.node_editor.node.item.params_item.ParameterItem;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Supplier;

public abstract class Node implements View.OnTouchListener, ConnectorCallback {

    Context context;
    LinesView linesView;
    NodeNav nav;
    ArrayList<NodeInput> nodeInput;
    ArrayList<NodeOutput> nodeOutput;
    ArrayList<ParameterItem> nodeParams;

    NodeCallbackListener listener;
    String id;
    RelativeLayout innerNode;
    private RelativeLayout node;
    private int xDelta, yDelta;
    private int leftMargin, topMargin;
    private int nodeWidth, nodeHeight = 700;
    private int nodeItemOrder = 1;
    private int inputDataReceived = 0;


    public Node(Context context, Integer leftMargin, Integer topMargin, @Nullable LinesView linesView, @Nullable NodeCallbackListener listener) {
        this.leftMargin = leftMargin;
        this.topMargin = topMargin;
        nodeWidth = NodeDimensionsCalculator.nodeWidth();

        this.listener = listener;
        this.linesView = linesView;
        this.context = Objects.requireNonNull(context);

        nodeOutput = new ArrayList<>();
        nodeInput = new ArrayList<>();
        nodeParams = new ArrayList<>();

        id = NodeItem.generateId();

        init(context);
    }

    public void setLinesView(LinesView linesView) {
        this.linesView = linesView;
    }

    public void setListener(NodeCallbackListener listener) {
        this.listener = listener;
        addListener(nodeInput);
        addListener(nodeOutput);
    }
    private <T extends NodeConnectorItem> void addListener(ArrayList<T> list){
        for (T item : list) {
            item.setListener(listener);
        }
    }

    public ArrayList<NodeOutput> getNodeOutput() {
        return nodeOutput;
    }

    /**
     * @return all inputs of node
     */
    public ArrayList<NodeInput> getNodeInput() {
        return nodeInput;
    }

    public NodeOutput addNodeOutput(Type type) {
        NodeOutput output = new NodeOutput(context, listener, this, nodeItemOrder, type);
        nodeOutput.add(output);
        nodeItemOrder++;
        return output;
    }

    public LinesView getLinesView() {
        return linesView;
    }

    public void setNodeOutput(ArrayList<NodeOutput> nodeOutput) {
        this.nodeOutput = nodeOutput;
    }

    public void setNodeInput(ArrayList<NodeInput> nodeInput) {
        this.nodeInput = nodeInput;
    }

    public <T extends ParameterItem> ParameterItem addNodeParam(Class<T> sup) {
        //nodeOutput.add(new NodeOutput(context, listener, this, 2, type));
        try {
            String myClassName = sup.getName();

            Class<?> myClass = Class.forName(myClassName);

            Constructor<T> ctr = (Constructor<T>) myClass.getConstructors()[0];

            T object = ctr.newInstance(context, this, nodeItemOrder);

            nodeParams.add(object);

            nodeItemOrder++;
            return object;
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        return null;
    }

    public NodeInput addNodeInput(Type type) {
        NodeInput input = new NodeInput(context, listener, this, nodeItemOrder, type);
        nodeInput.add(input);
        nodeItemOrder++;
        return input;
    }

    public void setOnChangedStateListener(NodeCallbackListener listener) {
        this.listener = listener;
    }

    public abstract CustomNodes getType();

    public NodeInput hoveringOn(int innerX, int innerY) {

        for (NodeInput input : nodeInput) {

            if (innerY > input.getOrder() * NodeDimensionsCalculator.nodeItemHeight()) {

                if (innerY < (input.getOrder() + 1) * NodeDimensionsCalculator.nodeItemHeight()) {

                    return input;
                }
            }
        }
        return null;
    }

    public boolean isStartingNode() {
        return nodeInput.size() == 0;
    }

    public NodeNav getNav() {
        return nav;
    }

    void init(Context context) {
        node = new RelativeLayout(context);
        node.setClipChildren(false);
        RelativeLayout.LayoutParams nodeParams = new RelativeLayout.LayoutParams(nodeWidth, nodeHeight);

        node.setLayoutParams(nodeParams);
        node.setBackgroundResource(R.drawable.back_node);
//        node.setElevation(20f);
        node.setOutlineProvider(ViewOutlineProvider.PADDED_BOUNDS);

        innerNode = new RelativeLayout(context);

        innerNode.setClipChildren(false);
        innerNode.setBackgroundResource(R.drawable.node);
        node.addView(innerNode);

        nav = new NodeNav(context);
        nav.setOnTouchListener(this);

        innerNode.addView(nav);

        setPosition(leftMargin, topMargin);


    }



    public void build() {

        for (NodeInput node : nodeInput) {
            innerNode.addView(node.getView());
        }

        for (ParameterItem node : nodeParams) {
            innerNode.addView(node.getView());
        }

        for (NodeOutput node : nodeOutput) {
            innerNode.addView(node.getView());
        }

        nodeHeight = NodeDimensionsCalculator.nodeItemHeight() * (nodeItemOrder + 1) + 50;

        node.setLayoutParams(new RelativeLayout.LayoutParams(nodeWidth, nodeHeight));

        RelativeLayout.LayoutParams innerViewParams = new RelativeLayout.LayoutParams(nodeWidth - 50, nodeHeight - 50);
        int innerMargin = NodeDimensionsCalculator.innerNodeMargin() / 2;
        innerViewParams.setMargins(innerMargin, innerMargin, 0, 0);
        innerNode.setLayoutParams(innerViewParams);

        setPosition(leftMargin, topMargin);
    }

    private void updatePositionVars() {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) node.getLayoutParams();
        leftMargin = params.leftMargin;
        topMargin = params.topMargin;
        for (NodeOutput node : nodeOutput) {
            node.updatePositionVars();
        }
        for (NodeInput node : nodeInput) {
            node.updatePositionVars();
        }

    }

    public int getLeftMargin() {
        return leftMargin;
    }

    public int getTopMargin() {
        return topMargin;
    }

    private Pair<Integer, Integer> getPosition() {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) node.getLayoutParams();
        return new Pair<>(params.leftMargin, params.topMargin);
    }

    private Pair<Integer, Integer> getSize() {
        return new Pair<>(nodeWidth, nodeHeight);
    }

    public void setPosition(int leftMargin, int topMargin) {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) node.getLayoutParams();
        params.leftMargin = leftMargin;
        params.topMargin = topMargin;
    }

    public RelativeLayout getNode() {
        return node;
    }

    private void callCallback() {
        if (listener != null)
            listener.onMoved(this);
    }

    public int getNodeWidth() {
        return nodeWidth;
    }

    public int getNodeHeight() {
        return nodeHeight;
    }

    public NodeInput getNodeInputBy(LinePoint point) {
        for (NodeInput input : getNodeInput()) {
            if (input.getPoint().equals(point)) {
                return input;
            }
        }
        return null;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        int rawX = (int) motionEvent.getRawX();
        int rawY = (int) motionEvent.getRawY();

        RelativeLayout.LayoutParams params =
                (RelativeLayout.LayoutParams) node.getLayoutParams();

        switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:

                xDelta = rawX - params.leftMargin;
                yDelta = rawY - params.topMargin;
                break;

            case MotionEvent.ACTION_MOVE:
                RelativeLayout.LayoutParams layoutParams =
                        (RelativeLayout.LayoutParams) node.getLayoutParams();
                layoutParams.leftMargin = rawX - xDelta;
                layoutParams.topMargin = rawY - yDelta;

                node.setLayoutParams(layoutParams);

                break;
        }

        updatePositionVars();

        callCallback();

        node.bringToFront();

        linesView.invalidate();

        //linePoint.setPosition(leftMargin, topMargin);

        return true;
    }

    @Override
    public void onItemConnect(NodeConnectorItem item) {

    }

    @Override
    public void dataInInputSent(String data, NodeInput input) {
        inputDataReceived++;
        if (inputDataReceived >= getNodeInput().size()) {
            process();
            sendData();
            inputDataReceived = 0;
        }

    }


    public String getId() {
        return "NodeEntity" + id;
    }

    public abstract void process();

    public abstract void sendData();


    //TODO xxx
    public void updateNodeCordinates(){

    };
}

