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

package jhilbert.scanners.impl;

import junit.framework.TestCase;

public class CharTest extends TestCase {

	public void testHashmark() throws Exception {
		assertEquals(Char.Class.HASHMARK,
			new Char('#').getCharClass());
	}

	public void testAscii() throws Exception {
		assertEquals(Char.Class.ATOM,
			new Char('a').getCharClass());
	}

	public void testEndOfFile() throws Exception {
		assertEquals(Char.Class.EOF,
			new Char(-1).getCharClass());
	}

	public void testPerpendicular() throws Exception {
		assertEquals(Char.Class.ATOM,
			new Char('\u27c2').getCharClass());
	}

	public void
	testCharacterAddedByAFutureVersionOfUnicodeBeforeJavaIsUpdated()
	throws Exception {
		assertEquals(Char.Class.ATOM,
				new Char('\u0e76').getCharClass());
	}

}
