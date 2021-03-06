/*******************************************************************************
 * Copyright (c) 2017 itemis AG and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthias Wienand (itemis AG) - initial API and implementation
 *     Tamas Miklossy   (itemis AG) - minor refactorings
 *     
 *******************************************************************************/
package org.eclipse.gef.dot.internal.language.validation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.gef.dot.internal.language.htmllabel.HtmlAttr;
import org.eclipse.gef.dot.internal.language.htmllabel.HtmlTag;
import org.eclipse.gef.dot.internal.language.htmllabel.HtmllabelPackage;
import org.eclipse.xtext.validation.Check;

/**
 * This class contains custom validation rules.
 *
 * See
 * https://www.eclipse.org/Xtext/documentation/303_runtime_concepts.html#validation
 */
public class DotHtmlLabelJavaValidator extends
		org.eclipse.gef.dot.internal.language.validation.AbstractDotHtmlLabelJavaValidator {

	private static final String ROOT_TAG_KEY = "ROOT";
	private static final Set<String> ALL_TAGS = new HashSet<>();
	private static final Map<String, Set<String>> validTags = new HashMap<>();
	private static final Map<String, Set<String>> allowedParents = new HashMap<>();
	private static final Map<String, Set<String>> validAttributes = new HashMap<>();

	static {
		// specify allowed top-level tags
		validTags.put(ROOT_TAG_KEY,
				new HashSet<>(Arrays.asList(new String[] { "BR", "FONT", "I",
						"B", "U", "O", "SUB", "SUP", "S", "TABLE" })));
		// add allowed nested tags
		validTags.put("TABLE", new HashSet<>(Arrays.asList("HR", "TR")));
		validTags.put("TR", new HashSet<>(Arrays.asList("VR", "TD")));
		validTags.put("TD", new HashSet<>(Arrays.asList("IMG", "BR", "FONT",
				"I", "B", "U", "O", "SUB", "SUP", "S", "TABLE")));

		// find all tags
		for (Set<String> ts : validTags.values()) {
			ALL_TAGS.addAll(ts);
		}

		// compute allowed parents for each tag
		for (String tag : ALL_TAGS) {
			allowedParents.put(tag, new HashSet<>());
		}
		for (String parent : validTags.keySet()) {
			for (String tag : validTags.get(parent)) {
				allowedParents.get(tag).add(parent);
			}
		}

		// specify tags that can have attributes
		for (String t : new String[] { "TABLE", "TD", "FONT", "BR", "IMG" }) {
			validAttributes.put(t, new HashSet<>());
		}
		// add allowed attributes
		validAttributes.get("TABLE")
				.addAll(Arrays.asList("ALIGN", "BGCOLOR", "BORDER",
						"CELLBORDER", "CELLPADDING", "CELLSPACING", "COLOR",
						"COLUMNS", "FIXEDSIZE", "GRADIENTANGLE", "HEIGHT",
						"HREF", "ID", "PORT", "ROWS", "SIDES", "STYLE",
						"TARGET", "TITLE", "TOOLTIP", "VALIGN", "WIDTH"));
		validAttributes.get("TD")
				.addAll(Arrays.asList("ALIGN", "BALIGN", "BGCOLOR", "BORDER",
						"CELLPADDING", "CELLSPACING", "COLOR", "COLSPAN",
						"FIXEDSIZE", "GRADIENTANGLE", "HEIGHT", "HREF", "ID",
						"PORT", "ROWSPAN", "SIDES", "STYLE", "TARGET", "TITLE",
						"TOOLTIP", "VALIGN", "WIDTH"));
		validAttributes.get("FONT")
				.addAll(Arrays.asList("COLOR", "FACE", "POINT"));
		validAttributes.get("BR").addAll(Arrays.asList("ALIGN"));
		validAttributes.get("IMG").addAll(Arrays.asList("SCALE", "SRC"));
	}

	/**
	 * Checks if the given {@link HtmlTag} is properly closed. Generates errors
	 * if the html tag is not closed properly.
	 * 
	 * @param tag
	 *            The {@link HtmlTag} to check.
	 */
	@Check
	public void checkTagIsClosed(HtmlTag tag) {
		if (!tag.getName().toUpperCase()
				.equals(tag.getCloseName().toUpperCase())) {
			error("Tag '<" + tag.getName() + ">' is not closed (expected '</"
					+ tag.getName() + ">' but got '</" + tag.getCloseName()
					+ ">').", HtmllabelPackage.Literals.HTML_TAG__CLOSE_NAME);
		}
	}

	/**
	 * Checks if the given {@link HtmlTag} is valid w.r.t. its parent (not all
	 * tags are allowed on all nesting levels). Generates warnings when the
	 * given {@link HtmlTag} is not supported by Graphviz w.r.t. its parent.
	 * 
	 * @param tag
	 *            The {@link HtmlTag} to check.
	 */
	@Check
	public void checkTagNameIsValid(HtmlTag tag) {
		String tagName = tag.getName();
		if (!ALL_TAGS.contains(tagName.toUpperCase())) {
			warning("Tag '<" + tagName + ">' is not supported.",
					HtmllabelPackage.Literals.HTML_TAG__NAME);
		} else {
			// find parent tag
			EObject container = tag.eContainer().eContainer();
			HtmlTag parent = null;
			if (container instanceof HtmlTag) {
				parent = (HtmlTag) container;
			}

			// check if tag allowed inside parent or "root" if we could not find
			// a parent
			String parentName = parent == null ? ROOT_TAG_KEY
					: parent.getName();
			if (!validTags.containsKey(parentName.toUpperCase())) {
				throw new IllegalStateException("Parent tag is unknown.");
			}
			if (!validTags.get(parentName.toUpperCase())
					.contains(tagName.toUpperCase())) {
				warning("Tag '<" + tagName + ">' is not allowed inside '<"
						+ parentName + ">', but only inside '<"
						+ String.join(">', '<",
								allowedParents.get(tagName.toUpperCase()))
						+ ">'.", HtmllabelPackage.Literals.HTML_TAG__NAME);
			}
		}
	}

	/**
	 * Checks if the given {@link HtmlAttr} is valid w.r.t. its tag (only
	 * certain attributes are supported by the individual tags). Generates
	 * warnings if the {@link HtmlAttr} is not supported by Graphviz w.r.t. its
	 * tag.
	 * 
	 * @param attr
	 *            The {@link HtmlAttr} to check.
	 */
	@Check
	public void checkAttributeNameIsValid(HtmlAttr attr) {
		String attrName = attr.getName();
		EObject container = attr.eContainer();
		if (container instanceof HtmlTag) {
			HtmlTag tag = (HtmlTag) container;
			if (tag != null) {
				String tagName = tag.getName();
				if (!validAttributes.containsKey(tagName.toUpperCase())
						|| !validAttributes.get(tagName.toUpperCase())
								.contains(attrName.toUpperCase())) {
					warning("Attribute '" + attrName
							+ "' is not allowed inside '<" + tagName + ">'.",
							HtmllabelPackage.Literals.HTML_ATTR__NAME);
				}
			}
		}
	}
}
