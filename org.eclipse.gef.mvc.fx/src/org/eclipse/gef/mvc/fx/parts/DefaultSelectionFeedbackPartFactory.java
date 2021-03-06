/*******************************************************************************
 * Copyright (c) 2016 itemis AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthias Wienand (itemis AG) - initial API and implementation
 *
 *******************************************************************************/
package org.eclipse.gef.mvc.fx.parts;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.gef.common.adapt.AdapterKey;
import org.eclipse.gef.fx.anchors.DynamicAnchor.AnchorageReferenceGeometry;
import org.eclipse.gef.fx.anchors.DynamicAnchor.AnchoredReferencePoint;
import org.eclipse.gef.fx.anchors.IComputationStrategy;
import org.eclipse.gef.fx.anchors.ProjectionStrategy;
import org.eclipse.gef.fx.nodes.Connection;
import org.eclipse.gef.fx.utils.NodeUtils;
import org.eclipse.gef.geometry.planar.IGeometry;
import org.eclipse.gef.geometry.planar.Line;
import org.eclipse.gef.geometry.planar.Point;

import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;

import javafx.scene.Node;
import javafx.scene.paint.Color;

/**
 *
 * @author mwienand
 *
 */
public class DefaultSelectionFeedbackPartFactory
		implements IFeedbackPartFactory {

	/**
	 * The binding name for the primary selection color.
	 */
	public static final String PRIMARY_SELECTION_FEEDBACK_COLOR_PROVIDER = "PRIMARY_SELECTION_FEEDBACK_COLOR_PROVIDER";

	/**
	 * Defines the default {@link Color} for primary selection feedback.
	 */
	public static final Color DEFAULT_PRIMARY_SELECTION_FEEDBACK_COLOR = Color
			.web("#3f51b5");

	/**
	 * The binding name for the secondary selection color.
	 */
	public static final String SECONDARY_SELECTION_FEEDBACK_COLOR_PROVIDER = "SECONDARY_SELECTION_FEEDBACK_COLOR_PROVIDER";

	/**
	 * Defines the default {@link Color} for secondary selection feedback.
	 */
	public static final Color DEFAULT_SECONDARY_SELECTION_FEEDBACK_COLOR = Color
			.web("#9fa8da");

	/**
	 * The role name for the <code>Provider&lt;IGeometry&gt;</code> that will be
	 * used to generate selection feedback.
	 */
	public static final String SELECTION_FEEDBACK_GEOMETRY_PROVIDER = "SELECTION_FEEDBACK_GEOMETRY_PROVIDER";

	/**
	 * The role name for the <code>Provider&lt;IGeometry&gt;</code> that will be
	 * used to generate selection link feedback.
	 */
	public static final String SELECTION_LINK_FEEDBACK_GEOMETRY_PROVIDER = "SELECTION_LINK_FEEDBACK_GEOMETRY_PROVIDER";

	@Inject
	private Injector injector;

	@Override
	public List<IFeedbackPart<? extends Node>> createFeedbackParts(
			List<? extends IVisualPart<? extends Node>> targets,
			Map<Object, Object> contextMap) {
		// check that we have targets
		if (targets == null || targets.isEmpty()) {
			throw new IllegalArgumentException(
					"Part factory is called without targets.");
		}

		// single selection, create selection feedback based on geometry
		List<IFeedbackPart<? extends Node>> feedbackParts = new ArrayList<>();

		// selection outline feedback
		final IVisualPart<? extends Node> target = targets.iterator().next();
		@SuppressWarnings("serial")
		final Provider<? extends IGeometry> selectionFeedbackGeometryProvider = target
				.getAdapter(AdapterKey
						.get(new TypeToken<Provider<? extends IGeometry>>() {
						}, SELECTION_FEEDBACK_GEOMETRY_PROVIDER));
		if (selectionFeedbackGeometryProvider != null) {
			Provider<IGeometry> geometryInSceneProvider = new Provider<IGeometry>() {
				@Override
				public IGeometry get() {
					return NodeUtils.localToScene(target.getVisual(),
							selectionFeedbackGeometryProvider.get());
				}
			};
			SelectionFeedbackPart selectionFeedbackPart = injector
					.getInstance(SelectionFeedbackPart.class);
			selectionFeedbackPart.setGeometryProvider(geometryInSceneProvider);
			feedbackParts.add(selectionFeedbackPart);
		}

		// selection link feedback parts
		for (IVisualPart<? extends Node> t : targets) {
			if (!t.getAnchoragesUnmodifiable().isEmpty()) {
				for (Entry<IVisualPart<? extends Node>, String> entry : t
						.getAnchoragesUnmodifiable().entries()) {
					if (entry.getKey() instanceof IVisualPart) {
						IFeedbackPart<? extends Node> anchorLinkFeedbackPart = createLinkFeedbackPart(
								t, entry.getKey(), entry.getValue());
						if (anchorLinkFeedbackPart != null) {
							feedbackParts.add(anchorLinkFeedbackPart);
						}
					}
				}
			}
		}

		return feedbackParts;
	}

	/**
	 * Creates a dotted feedback-line between an anchored part and its
	 * anchorage.
	 *
	 * @param anchored
	 *            The anchored {@link IVisualPart}.
	 * @param anchorage
	 *            The anchorage {@link IVisualPart}.
	 * @param anchorageRole
	 *            The role under which the anchorage is stored at the anchored.
	 * @return The {@link IFeedbackPart} for this anchor link, or
	 *         <code>null</code> if no feedback should be rendered.
	 */
	@SuppressWarnings("serial")
	private IFeedbackPart<? extends Node> createLinkFeedbackPart(
			final IVisualPart<? extends Node> anchored,
			final IVisualPart<? extends Node> anchorage, String anchorageRole) {
		// only show link feedback when anchored is no connection
		if (!(anchored.getVisual() instanceof Connection)) {
			final Provider<? extends IGeometry> anchorageGeometryProvider = anchorage
					.getAdapter(AdapterKey.get(
							new TypeToken<Provider<? extends IGeometry>>() {
							}, SELECTION_LINK_FEEDBACK_GEOMETRY_PROVIDER));
			final Provider<? extends IGeometry> anchoredGeometryProvider = anchored
					.getAdapter(AdapterKey.get(
							new TypeToken<Provider<? extends IGeometry>>() {
							}, SELECTION_LINK_FEEDBACK_GEOMETRY_PROVIDER));
			// only show anchor link feedback if anchorage and anchored provider
			// is not null
			if (anchoredGeometryProvider != null
					&& anchorageGeometryProvider != null) {
				Provider<IGeometry> linkFeedbackGeometryProvider = new Provider<IGeometry>() {
					// TODO (#471628): inject; maybe extend IComputationStrategy
					// interface
					private final ProjectionStrategy computationStrategy = new ProjectionStrategy();

					private Point computePosition(Node n1, IGeometry n1Geometry,
							Node n2, IGeometry n2Geometry) {
						Point n2RefPoint = n2Geometry.getBounds().getCenter();
						// TODO: let computation strategy initialize the
						// parameters, then populate them
						Set<IComputationStrategy.Parameter<?>> parameters = new HashSet<>();
						parameters.add(
								new AnchorageReferenceGeometry(n1Geometry));
						parameters.add(new AnchoredReferencePoint(n2RefPoint));
						return computationStrategy.computePositionInScene(n1,
								n2, parameters);
					}

					@Override
					public IGeometry get() {
						// get anchored visual and geometry
						Node anchoredVisual = anchored.getVisual();
						IGeometry anchoredGeometryInLocal = anchoredGeometryProvider
								.get();

						// get anchorage visual and geometry
						Node anchorageVisual = anchorage.getVisual();
						IGeometry anchorageGeometryInLocal = anchorageGeometryProvider
								.get();

						// determine link source point
						Point sourcePointInScene = computePosition(
								anchoredVisual, anchoredGeometryInLocal,
								anchorageVisual, anchorageGeometryInLocal);

						// determine link target point
						Point targetPointInScene = computePosition(
								anchorageVisual, anchorageGeometryInLocal,
								anchoredVisual, anchoredGeometryInLocal);

						// construct link line
						return new Line(sourcePointInScene, targetPointInScene);
					}
				};
				SelectionLinkFeedbackPart part = injector
						.getInstance(SelectionLinkFeedbackPart.class);
				part.setGeometryProvider(linkFeedbackGeometryProvider);
				return part;
			}
		}
		return null;
	}

}
