/*******************************************************************************
 * Copyright (c) 2015, 2016 itemis AG and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Alexander Nyßen (itemis AG) - initial API and implementation
 *     Tamas Miklossy  (itemis AG) - merge DotInterpreter into DotImport (bug #491261)
 *
 *******************************************************************************/
package org.eclipse.gef.dot.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ DotImportTests.class, DotExportTests.class,
		DotExecutableUtilsTests.class, DotExtractorTests.class,
		DotAttributesTests.class, DotParserTests.class, DotEscStringTests.class,
		DotHighlightingTests.class, DotValidatorTests.class,
		DotContentAssistTests.class, DotBSplineInterpolatorTests.class,
		DotHtmlLabelTests.class })
public class AllUiTests {

}
