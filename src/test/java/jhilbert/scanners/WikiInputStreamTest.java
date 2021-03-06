/*
    JHilbert, a verifier for collaborative theorem proving

    Copyright © 2008, 2009, 2011 The JHilbert Authors
      See the AUTHORS file for the list of JHilbert authors.
      See the commit logs ("git log") for a list of individual contributions.

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

    You may contact the author on this Wiki page:
    http://www.wikiproofs.de/w/index.php?title=User_talk:GrafZahl
*/

package jhilbert.scanners;

import java.io.ByteArrayInputStream;

import junit.framework.TestCase;

public class WikiInputStreamTest extends TestCase {

	public void testPageWithoutJh() throws Exception {
		assertEquals("", WikiInputStream.read(
			"This is some [[wikitext]] {{and a template}}."));
	}

	public void testFindJhSections() throws Exception {
		assertEquals("\nvar (\nformula p)", WikiInputStream.read(
			"Non-jhilbert <jh>var (</jh> and then <jh>formula p)</jh>"));
	}

	public void testOpenJhNoCloseJh() throws Exception {
		try {
			WikiInputStream.read("<jh>thm (");
			fail();
		}
		catch (Exception expected) {
			assertEquals("Missing </jh> tag", expected.getMessage());
		}
	}

	public void testCloseJhBeforeJh() throws Exception {
		try {
			WikiInputStream.read("</jh>");
			fail();
		}
		catch (Exception expected) {
			assertEquals("Found </jh> tag without matching <jh> tag",
				expected.getMessage());
		}
	}

	public void testCloseJhWhenAlreadyClosed() throws Exception {
		try {
			WikiInputStream.read("<jh></jh></jh>");
			fail();
		}
		catch (Exception expected) {
			assertEquals("Found </jh> tag without matching <jh> tag",
				expected.getMessage());
		}
	}

	public void testOpenJhWhenAlreadyOpen() throws Exception {
		try {
			WikiInputStream.read("<jh>\n<jh>\n</jh>\n");
			fail();
		}
		catch (Exception expected) {
			assertEquals("Found <jh> tag inside <jh> tag", expected.getMessage());
		}
	}

	public void testNoErrorsExpected() throws Exception {
		WikiInputStream wiki = WikiInputStream.create(new ByteArrayInputStream(
			"Hi, I'm a page.\n<jh>\n</jh>\n".getBytes("UTF-8")));
		assertEquals(0, wiki.expectedErrors().size());
	}

	public void testErrorExpectedBeforeFirstStartTag() throws Exception {
		WikiInputStream wiki = WikiInputStream.create(new ByteArrayInputStream(
			"{{error expected|it's broke}}\n<jh>\n</jh>\n"
		    .getBytes("UTF-8")));
		assertEquals(1, wiki.expectedErrors().size());
		assertEquals("it's broke", wiki.expectedErrors().get(0));
	}

	public void testTwoExpectedErrors() throws Exception {
		WikiInputStream wiki = WikiInputStream.create(new ByteArrayInputStream(
				("{{error expected|it's heavy}}\n" +
				"{{error expected|it's ugly}}\n")
			    .getBytes("UTF-8")));
		assertEquals(2, wiki.expectedErrors().size());
		assertEquals("it's heavy", wiki.expectedErrors().get(0));
		assertEquals("it's ugly", wiki.expectedErrors().get(1));
	}

	public void testErrorExpectedBetweenTags() throws Exception {
		WikiInputStream wiki = WikiInputStream.create(new ByteArrayInputStream(
			("<jh>\n" +
			"</jh>\n" +
			"Foo. {{error expected|syntax error}}\n" +
			"<jh>\n" +
			"the bad syntax\n" +
			"</jh>\n")
			.getBytes("UTF-8")));
		assertEquals(1, wiki.expectedErrors().size());
		assertEquals("syntax error", wiki.expectedErrors().get(0));
	}

	public void testDoNotRecognizeErrorExpectedInJh() throws Exception {
		WikiInputStream wiki = WikiInputStream.create(new ByteArrayInputStream(
			("<jh>\n" +
			"# We would add {{error expected|foo}} if this were really bad\n" +
			"</jh>\n")
			.getBytes("UTF-8")));
		assertEquals(0, wiki.expectedErrors().size());
	}

	public void testErrorExpectedAfterLastCloseTag() throws Exception {
		WikiInputStream wiki = WikiInputStream.create(new ByteArrayInputStream(
			("<jh>\n" +
			"</jh>\n" +
			"{{error expected|it's broke}}\n\n")
		    .getBytes("UTF-8")));
		assertEquals(1, wiki.expectedErrors().size());
		assertEquals("it's broke", wiki.expectedErrors().get(0));
	}

}
