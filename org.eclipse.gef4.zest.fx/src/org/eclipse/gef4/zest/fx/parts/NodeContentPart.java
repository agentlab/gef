/*******************************************************************************
 * Copyright (c) 2014 itemis AG and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthias Wienand (itemis AG) - initial API & implementation
 *
 *******************************************************************************/
package org.eclipse.gef4.zest.fx.parts;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Bounds;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.transform.Transform;

import org.eclipse.gef4.geometry.convert.fx.JavaFX2Geometry;
import org.eclipse.gef4.graph.Graph;
import org.eclipse.gef4.mvc.fx.parts.AbstractFXContentPart;
import org.eclipse.gef4.mvc.parts.IVisualPart;
import org.eclipse.gef4.zest.fx.ZestProperties;

public class NodeContentPart extends AbstractFXContentPart<Group> {

	/**
	 * JavaFX Node displaying a small icon representing a nested graph.
	 */
	public static class NestedGraphIcon extends Group {
		{
			Circle n0 = node(-20, -20);
			Circle n1 = node(-10, 10);
			Circle n2 = node(5, -15);
			Circle n3 = node(15, -25);
			Circle n4 = node(20, 5);
			getChildren().addAll(edge(n0, n1), edge(n1, n2), edge(n2, n3),
					edge(n3, n4), edge(n1, n4), n0, n1, n2, n3, n4);
		}

		private Node edge(Circle n, Circle m) {
			Line line = new Line(n.getCenterX(), n.getCenterY(),
					m.getCenterX(), m.getCenterY());
			line.setStroke(Color.BLACK);
			return line;
		}

		private Circle node(double x, double y) {
			return new Circle(x, y, 5, Color.BLACK);
		}
	}

	// defaults
	protected static final double DEFAULT_PADDING = 5;
	protected static final double ZOOMLEVEL_SHOW_NESTED_GRAPH = 2;
	protected static final double DEFAULT_CHILDREN_PANE_WIDTH = 300;
	protected static final double DEFAULT_CHILDREN_PANE_HEIGHT = 300;
	protected static final double CHILDREN_PANE_WIDTH_THRESHOLD = 100;
	protected static final double CHILDREN_PANE_HEIGHT_THRESHOLD = 100;

	// CSS classes for styling nodes
	public static final String CSS_CLASS = "node";
	public static final String CSS_CLASS_SHAPE = "shape";
	public static final String CSS_CLASS_LABEL = "label";
	public static final String CSS_CLASS_ICON = "icon";

	private static final String NODE_LABEL_EMPTY = "-";

	private Text labelText;
	private ImageView iconImageView;
	private Node nestedGraphIcon;
	private StackPane nestedContentStackPane;
	private Pane nestedChildrenPane;
	private int originalIndex = -1;
	private Bounds originalBounds = null;
	private Tooltip tooltipNode;
	private HBox hbox;
	private VBox vbox;
	private Rectangle rect;

	private EventHandler<? super MouseEvent> mouseHandler = new EventHandler<MouseEvent>() {
		@Override
		public void handle(MouseEvent event) {
			EventType<? extends Event> type = event.getEventType();
			if (type.equals(MouseEvent.MOUSE_ENTERED)
					|| type.equals(MouseEvent.MOUSE_EXITED)) {
				refreshVisual();
			} else if (type.equals(MouseEvent.MOUSE_MOVED)
					|| type.equals(MouseEvent.MOUSE_DRAGGED)) {
				if (originalBounds != null) {
					if (!originalBounds.contains(event.getSceneX(),
							event.getSceneY())) {
						// unhover the visual by making it mouse transparent
						getVisual().setMouseTransparent(true);
						// this will result in a MOUSE_EXITED event being fired,
						// which will lead to a refreshVisual() call, which will
						// update the visualization
					}
				}
			}
		}
	};
	private PropertyChangeListener nodeAttributesPropertyChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			if (org.eclipse.gef4.graph.Node.ATTRIBUTES_PROPERTY.equals(evt
					.getPropertyName())) {
				refreshVisual();
			}
		}
	};

	@Override
	protected void addChildVisual(IVisualPart<Node, ? extends Node> child,
			int index) {
		getNestedChildrenPane().getChildren().add(index, child.getVisual());
	}

	protected Pane createNestedContentPane() {
		Pane pane = new Pane();
		pane.setStyle("-fx-background-color: white;");
		pane.setScaleX(0.25);
		pane.setScaleY(0.25);
		pane.setPrefSize(0, 0);
		return pane;
	}

	protected StackPane createNestedContentStackPane(Pane nestedContentPane) {
		StackPane stackPane = new StackPane();
		stackPane.getChildren().add(new Group(nestedContentPane));
		return stackPane;
	}

	/**
	 * Creates the node visual. The given {@link ImageView}, {@link Text}, and
	 * {@link StackPane} are inserted into that node visual to display the
	 * node's icon, label and nested children, respectively. The node visual
	 * needs to be inserted into the given {@link Group}.
	 *
	 * @param group
	 *            This node's visual.
	 * @param iconImageView
	 *            The {@link ImageView} displaying the node's icon.
	 * @param labelText
	 *            The {@link Text} displaying the node's label.
	 * @param nestedContentStackPane
	 *            The {@link StackPane} displaying the node's nested content.
	 */
	protected void createNodeVisual(final Group group, final Rectangle rect,
			final ImageView iconImageView, final Text labelText,
			final StackPane nestedContentStackPane) {
		// put image and text next to each other at the top of the node
		hbox = new HBox();
		hbox.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		hbox.getChildren().addAll(iconImageView, labelText);

		// put nested content stack pane below image and text
		vbox = new VBox();
		vbox.setMouseTransparent(true);
		vbox.getChildren().addAll(hbox, nestedContentStackPane);

		// expand box depending on content size
		vbox.layoutBoundsProperty().addListener(new ChangeListener<Bounds>() {
			@Override
			public void changed(ObservableValue<? extends Bounds> observable,
					Bounds oldValue, Bounds newValue) {
				vbox.setTranslateX(getPadding());
				vbox.setTranslateY(getPadding());
				rect.setWidth(vbox.getWidth() + 2 * getPadding());
				rect.setHeight(vbox.getHeight() + 2 * getPadding());
			}
		});

		// place the box below the other visuals
		group.getChildren().addAll(rect, vbox);
	}

	@Override
	protected Group createVisual() {
		// container set-up
		final Group group = new Group() {
			@Override
			public boolean isResizable() {
				// every node is resizable when it contains a graph
				return isNesting();
			}

			@Override
			public void resize(double w, double h) {
				if (!isResizable()) {
					return;
				}

				// compute delta size, based on layout bounds
				Bounds layoutBounds = getLayoutBounds();
				resizeNestedGraphArea(w - layoutBounds.getWidth(), h
						- layoutBounds.getHeight());
			}
		};

		// create box for border and background
		rect = new Rectangle();
		rect.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.REFLECT,
				Arrays.asList(new Stop(0, new Color(1, 1, 1, 1)))));
		rect.setStroke(new Color(0, 0, 0, 1));
		rect.getStyleClass().add(CSS_CLASS_SHAPE);

		nestedChildrenPane = createNestedContentPane();
		nestedContentStackPane = createNestedContentStackPane(nestedChildrenPane);

		// initialize image view
		iconImageView = new ImageView();
		iconImageView.setImage(null);
		iconImageView.getStyleClass().add(CSS_CLASS_ICON);

		// initialize text
		labelText = new Text();
		labelText.setTextOrigin(VPos.TOP);
		labelText.setText(NODE_LABEL_EMPTY);
		labelText.getStyleClass().add(CSS_CLASS_LABEL);

		// build node visual
		createNodeVisual(group, rect, iconImageView, labelText,
				nestedContentStackPane);

		return group;
	}

	@Override
	protected void doActivate() {
		super.doActivate();
		getContent().addPropertyChangeListener(
				nodeAttributesPropertyChangeListener);
	}

	@Override
	protected void doDeactivate() {
		getContent().removePropertyChangeListener(
				nodeAttributesPropertyChangeListener);
		super.doDeactivate();
	}

	@Override
	public void doRefreshVisual(Group visual) {
		if (getContent() == null) {
			throw new IllegalStateException();
		}

		// set CSS class
		visual.getStyleClass().clear();
		visual.getStyleClass().add(CSS_CLASS);
		org.eclipse.gef4.graph.Node node = getContent();
		Map<String, Object> attrs = node.getAttrs();
		if (attrs.containsKey(ZestProperties.ELEMENT_CSS_CLASS)) {
			refreshCssClass(visual, ZestProperties.getCssClass(node));
		}

		// set CSS id
		String id = null;
		if (attrs.containsKey(ZestProperties.ELEMENT_CSS_ID)) {
			id = ZestProperties.getCssId(node);
		}
		visual.setId(id);

		// set CSS style
		if (attrs.containsKey(ZestProperties.NODE_RECT_CSS_STYLE)) {
			rect.setStyle(ZestProperties.getNodeRectCssStyle(node));
		}
		if (attrs.containsKey(ZestProperties.NODE_LABEL_CSS_STYLE)) {
			labelText.setStyle(ZestProperties.getNodeLabelCssStyle(node));
		}

		// determine label
		Object label = attrs.get(ZestProperties.ELEMENT_LABEL);
		// use id if no label is set
		if (label == null) {
			label = id;
		}
		// use the the DEFAULT_LABEL if no label is set
		String str = label instanceof String ? (String) label
				: label == null ? NODE_LABEL_EMPTY : label.toString();
		// eventually let the fisheye mode trim the label
		str = refreshFisheye(visual, attrs, str);
		refreshLabel(visual, str);

		refreshIcon(visual, attrs.get(ZestProperties.NODE_ICON));
		refreshNestedGraphArea(visual, isNesting());
		refreshTooltip(visual, attrs.get(ZestProperties.NODE_TOOLTIP));
	}

	@Override
	public org.eclipse.gef4.graph.Node getContent() {
		return (org.eclipse.gef4.graph.Node) super.getContent();
	}

	@Override
	public List<? extends Object> getContentChildren() {
		Graph nestedGraph = getContent().getNestedGraph();
		if (nestedGraph == null) {
			return Collections.emptyList();
		}
		// only show children when zoomed in
		Transform tx = getVisual().getLocalToSceneTransform();
		double scale = JavaFX2Geometry.toAffineTransform(tx).getScaleX();
		if (scale > ZOOMLEVEL_SHOW_NESTED_GRAPH) {
			hideNestedGraphIcon();
			return Collections.singletonList(nestedGraph);
		}
		// show an icon as a replacement when the zoom threshold is not reached
		showNestedGraphIcon();
		return Collections.emptyList();
	}

	protected ImageView getIconImageView() {
		return iconImageView;
	}

	protected Text getLabelText() {
		return labelText;
	}

	public Pane getNestedChildrenPane() {
		return nestedChildrenPane;
	}

	protected StackPane getNestedContentStackPane() {
		return nestedContentStackPane;
	}

	protected Node getNestedGraphIcon() {
		return nestedGraphIcon;
	}

	protected Rectangle getNodeRect() {
		return rect;
	}

	protected double getPadding() {
		return DEFAULT_PADDING;
	}

	protected void hideNestedGraphIcon() {
		if (getNestedGraphIcon() != null) {
			getNestedContentStackPane().getChildren().remove(
					getNestedGraphIcon());
			setNestedGraphIcon(null);
		}
	}

	protected boolean isNesting() {
		return getContent().getNestedGraph() != null;
	}

	protected void refreshCssClass(Group visual, String cssClass) {
		visual.getStyleClass().add(cssClass);
	}

	protected String refreshFisheye(Group visual, Map<String, Object> attrs,
			String str) {
		// limit label to first letter when in fisheye mode (and not hovered)
		Object fisheye = attrs.get(ZestProperties.NODE_FISHEYE);
		if (fisheye instanceof Boolean && (Boolean) fisheye) {
			// register mouse event listeners
			visual.addEventHandler(MouseEvent.ANY, mouseHandler);
			if (!visual.isHover()) {
				// limit label to first letter
				// TODO: hide image, hide children/graph icon
				str = str.substring(0, 1);
				restoreZOrder();
			} else {
				if (originalBounds == null) {
					originalBounds = visual.localToScene(visual
							.getLayoutBounds());
				}
				// TODO: show image, show children/graph icon
				// highlight this node by moving it to the front
				List<IVisualPart<Node, ? extends Node>> children = getParent()
						.getChildren();
				originalIndex = children.indexOf(this); // restore later
				getParent().reorderChild(this, children.size() - 1);
				visual.toFront();
			}
		} else {
			// TODO: show image, show children/graph icon
			restoreZOrder();
			visual.removeEventHandler(MouseEvent.ANY, mouseHandler);
		}
		return str;
	}

	protected void refreshIcon(Group visual, Object icon) {
		if (iconImageView.getImage() != icon && icon instanceof Image) {
			iconImageView.setImage((Image) icon);
		}
	}

	protected void refreshLabel(Group visual, String str) {
		if (!labelText.getText().equals(str)) {
			labelText.setText(str);
		}
	}

	/**
	 * When this node has a nested graph, space is reserved for it, so that the
	 * transition from an icon to the real graph will not change the node's
	 * size.
	 *
	 * @param visual
	 *            The visual of this part.
	 * @param isNesting
	 *            <code>true</code> if this node has a nested graph, otherwise
	 *            <code>false</code>.
	 */
	protected void refreshNestedGraphArea(Group visual, boolean isNesting) {
		Pane nestedContentPane = getNestedChildrenPane();
		if (isNesting) {
			if (nestedContentPane.getPrefWidth() == 0
					&& nestedContentPane.getPrefHeight() == 0) {
				// reserve space
				nestedContentPane.setPrefSize(DEFAULT_CHILDREN_PANE_WIDTH,
						DEFAULT_CHILDREN_PANE_HEIGHT);
				nestedContentPane.resize(DEFAULT_CHILDREN_PANE_WIDTH,
						DEFAULT_CHILDREN_PANE_HEIGHT);
			}
		} else {
			if (nestedContentPane.getPrefWidth() != 0
					|| nestedContentPane.getPrefHeight() != 0) {
				// no nested graph => do not waste space
				nestedContentPane.setPrefSize(0, 0);
				nestedContentPane.resize(0, 0);
			}
		}
	}

	protected void refreshTooltip(Group visual, Object tooltip) {
		if (tooltip instanceof String) {
			if (tooltipNode == null) {
				tooltipNode = new Tooltip((String) tooltip);
				Tooltip.install(visual, tooltipNode);
			} else {
				tooltipNode.setText((String) tooltip);
			}
		}
	}

	@Override
	protected void removeChildVisual(IVisualPart<Node, ? extends Node> child,
			int index) {
		getNestedChildrenPane().getChildren().remove(index);
	}

	/**
	 * Resizes the area for the graph nested in this node.
	 *
	 * @param dw
	 *            Delta width.
	 * @param dh
	 *            Delta height.
	 */
	protected void resizeNestedGraphArea(double dw, double dh) {
		// compute new size, taking into account the childrenPane scale
		Pane nestedContentPane = getNestedChildrenPane();
		double newWidth = nestedContentPane.getPrefWidth() + dw * 1
				/ nestedContentPane.getScaleX();
		double newHeight = nestedContentPane.getPrefHeight() + dh * 1
				/ nestedContentPane.getScaleY();

		// do not resize below threshold
		if (newWidth < CHILDREN_PANE_WIDTH_THRESHOLD) {
			newWidth = CHILDREN_PANE_WIDTH_THRESHOLD;
		}
		if (newHeight < CHILDREN_PANE_HEIGHT_THRESHOLD) {
			newHeight = CHILDREN_PANE_HEIGHT_THRESHOLD;
		}

		// compute relocation (node is oriented at center)
		double oldWidth = nestedContentPane.getWidth();
		double oldHeight = nestedContentPane.getHeight();
		dw = newWidth - oldWidth;
		dh = newHeight - oldHeight;
		double dx = nestedContentPane.getScaleX() * dw / 2;
		double dy = nestedContentPane.getScaleY() * dh / 2;

		// perform the resize
		nestedContentPane.setPrefSize(newWidth, newHeight);
		nestedContentPane.resize(newWidth, newHeight);

		// perform the relocation
		getVisual().setLayoutX(getVisual().getLayoutX() + dx);
		getVisual().setLayoutY(getVisual().getLayoutY() + dy);
	}

	private void restoreZOrder() {
		if (originalIndex >= 0) {
			getParent().reorderChild(this, originalIndex);
		}
		// make the visual hoverable by making it opaque for mouse events again
		getVisual().setMouseTransparent(false);
		// clear original bounds, so that they are recomputed
		originalBounds = null;
	}

	protected void setNestedGraphIcon(Node nestedGraphIcon) {
		this.nestedGraphIcon = nestedGraphIcon;
	}

	protected void showNestedGraphIcon() {
		if (getNestedGraphIcon() == null) {
			setNestedGraphIcon(new NestedGraphIcon());
			getNestedContentStackPane().getChildren().add(getNestedGraphIcon());
		}
	}

}
