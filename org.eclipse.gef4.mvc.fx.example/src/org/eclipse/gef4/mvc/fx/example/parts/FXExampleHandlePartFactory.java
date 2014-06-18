/*******************************************************************************
 * Copyright (c) 2014 itemis AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Alexander Nyßen (itemis AG) - initial API and implementation
 *     
 *******************************************************************************/
package org.eclipse.gef4.mvc.fx.example.parts;

import java.util.List;

import javafx.geometry.Pos;
import javafx.scene.Node;

import org.eclipse.gef4.geometry.planar.BezierCurve;
import org.eclipse.gef4.geometry.planar.ICurve;
import org.eclipse.gef4.geometry.planar.IGeometry;
import org.eclipse.gef4.mvc.fx.parts.FXDefaultHandlePartFactory;
import org.eclipse.gef4.mvc.fx.parts.FXSegmentHandlePart;
import org.eclipse.gef4.mvc.fx.policies.AbstractFXDragPolicy;
import org.eclipse.gef4.mvc.fx.policies.FXBendOnHandleDragPolicy;
import org.eclipse.gef4.mvc.fx.policies.FXReconnectOnHandleDragPolicy;
import org.eclipse.gef4.mvc.fx.policies.FXResizeRelocateOnHandleDragPolicy;
import org.eclipse.gef4.mvc.fx.policies.FXResizeRelocateOnHandleDragPolicy.ReferencePoint;
import org.eclipse.gef4.mvc.parts.IContentPart;
import org.eclipse.gef4.mvc.parts.IHandlePart;

import com.google.inject.Provider;

public class FXExampleHandlePartFactory extends FXDefaultHandlePartFactory {

	private List<IHandlePart<Node>> parts;

	@Override
	public IHandlePart<Node> createMultiSelectionCornerHandlePart(
			List<IContentPart<Node>> targets, Pos position) {
		IHandlePart<Node> part = super.createMultiSelectionCornerHandlePart(
				targets, position);
		part.setAdapter(AbstractFXDragPolicy.class,
				new FXResizeRelocateOnHandleDragPolicy(
						toReferencePoint(position)));
		return part;
	}

	private ReferencePoint toReferencePoint(Pos position) {
		switch (position) {
		case TOP_LEFT:
			return ReferencePoint.TOP_LEFT;
		case TOP_RIGHT:
			return ReferencePoint.TOP_RIGHT;
		case BOTTOM_LEFT:
			return ReferencePoint.BOTTOM_LEFT;
		case BOTTOM_RIGHT:
			return ReferencePoint.BOTTOM_RIGHT;
		default:
			throw new IllegalStateException(
					"Unknown Pos: <"
							+ position
							+ ">. Expected any of: TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT.");
		}
	}

	@Override
	protected List<IHandlePart<Node>> createCurveSelectionHandleParts(
			final IContentPart<Node> targetPart,
			Provider<IGeometry> handleGeometryProvider, IGeometry geom) {
		parts = super.createCurveSelectionHandleParts(targetPart,
				handleGeometryProvider, geom);

		// create mid point (insertion) handles
		BezierCurve[] beziers = ((ICurve) geom).toBezier();
		for (int i = 0; i < beziers.length; i++) {
			int segmentIndex = i;
			final FXSegmentHandlePart hp = new FXSegmentHandlePart(
					targetPart, handleGeometryProvider, segmentIndex, 0.5);
			hp.setAdapter(AbstractFXDragPolicy.class,
					new FXBendOnHandleDragPolicy());
			parts.add(hp);
		}

		return parts;
	}

	@Override
	public IHandlePart<Node> createCurveSelectionHandlePart(
			final IContentPart<Node> targetPart,
			final Provider<IGeometry> handleGeometryProvider,
			int segmentIndex, final boolean isEndPoint) {
		final FXSegmentHandlePart part = (FXSegmentHandlePart) super
				.createCurveSelectionHandlePart(targetPart,
						handleGeometryProvider, segmentIndex, isEndPoint);

		if (segmentIndex > 0 && !isEndPoint) {
			// make way points (middle segment vertices) draggable
			part.setAdapter(AbstractFXDragPolicy.class,
					new FXBendOnHandleDragPolicy());
		} else {
			// make end points reconnectable
			part.setAdapter(AbstractFXDragPolicy.class,
					new FXReconnectOnHandleDragPolicy(isEndPoint));
		}

		return part;
	}

	@Override
	public IHandlePart<Node> createShapeSelectionHandlePart(
			IContentPart<Node> targetPart,
			Provider<IGeometry> handleGeometryProvider, int vertexIndex) {
		IHandlePart<Node> part = super.createShapeSelectionHandlePart(
				targetPart, handleGeometryProvider, vertexIndex);
		part.setAdapter(AbstractFXDragPolicy.class,
				new FXResizeRelocateOnHandleDragPolicy(
						toReferencePoint(vertexIndex)));
		return part;
	}

	private ReferencePoint toReferencePoint(int vertexIndex) {
		switch (vertexIndex) {
		case 0:
			return ReferencePoint.TOP_LEFT;
		case 1:
			return ReferencePoint.TOP_RIGHT;
		case 2:
			return ReferencePoint.BOTTOM_RIGHT;
		case 3:
			return ReferencePoint.BOTTOM_LEFT;
		default:
			throw new IllegalStateException("Unsupported vertex index ("
					+ vertexIndex + "), expected 0 to 3.");
		}
	}

}
