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
package org.eclipse.gef.mvc.fx.policies;

import org.eclipse.gef.mvc.fx.tools.PinchSpreadTool;

import javafx.scene.input.ZoomEvent;

/**
 * An interaction policy that implements the {@link IOnPinchSpreadPolicy}
 * interface will be notified about touch pinch/spread events by the
 * {@link PinchSpreadTool}.
 *
 * @author mwienand
 *
 */
public interface IOnPinchSpreadPolicy extends IPolicy {

	/**
	 * Reaction to the unexpected finish of a pinch gesture.
	 */
	void abortZoom();

	/**
	 * Reaction to the finish of pinch (close fingers) gestures.
	 *
	 * @param e
	 *            The original {@link ZoomEvent}.
	 */
	void endZoom(ZoomEvent e);

	/**
	 * Reaction to the detection of pinch (close fingers) gestures.
	 *
	 * @param e
	 *            The original {@link ZoomEvent}.
	 */
	void startZoom(ZoomEvent e);

	/**
	 * Continuous reaction to pinch (close fingers) gestures. Called
	 * continuously on finger movement, after the gesture has been detected, and
	 * before it has been finished.
	 *
	 * @param e
	 *            The original {@link ZoomEvent}.
	 */
	void zoom(ZoomEvent e);

}