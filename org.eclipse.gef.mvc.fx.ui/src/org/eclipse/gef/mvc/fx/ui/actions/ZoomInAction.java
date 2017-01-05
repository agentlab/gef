/*******************************************************************************
 * Copyright (c) 2017 itemis AG and others.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthias Wienand (itemis AG) - initial API and implementation
 *
 *******************************************************************************/
package org.eclipse.gef.mvc.fx.ui.actions;

/**
 * The {@link ZoomInAction} is an {@link AbstractZoomAction} that increases the
 * zoom level by multiplying it with <code>1.25</code> while preserving the
 * center of the diagram.
 *
 * @author mwienand
 *
 */
public class ZoomInAction extends AbstractZoomAction {

	/**
	 *
	 */
	public ZoomInAction() {
		super("Zoom In");
	}

	@Override
	protected double determineZoomFactor(double currentZoomFactor) {
		return currentZoomFactor * 1.25;
	}
}