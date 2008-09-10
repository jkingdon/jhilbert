/*
    JHilbert, a verifier for collaborative theorem proving
    Copyright © 2008 Alexander Klauer

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

    You may contact the author on these Wiki pages:
    http://planetx.cc.vt.edu/AsteroidMeta//GrafZahl (preferred)
    http://en.wikisource.org/wiki/User_talk:GrafZahl
*/

package jhilbert.data.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jhilbert.data.DataException;
import jhilbert.data.Kind;
import jhilbert.data.ModuleData;
import jhilbert.data.Parameter;
import jhilbert.data.Symbol;
import jhilbert.data.Variable;
import jhilbert.data.impl.KindImpl;
import org.apache.log4j.Logger;

/**
 * Data collected in a proof module.
 */
final class ModuleDataImpl extends DataImpl implements ModuleData {

	/**
	 * Logger for this class.
	 */
	private static final Logger logger = Logger.getLogger(ModuleDataImpl.class);

	/**
	 * Kinds.
	 */
	private final Map<String, Kind> kinds;

	/**
	 * Creates a new instance of module data.
	 */
	public ModuleDataImpl() {
		super();
		kinds = new HashMap();
		terms = new HashMap();
	}

	/**
	 * Defines a new kind.
	 *
	 * @param name name of the kind to be defined (must not be <code>null</code>).
	 *
	 * @throws DataException if a kind with the specified name already exists.
	 */
	void defineKind(final String name) throws DataException {
		assert (name != null): "Supplied kind name is null.";
		if (kinds.containsKey(name)) {
			logger.error("Kind " + name + " already defined.");
			throw new DataException("Kind already defined", name);
		}
		kinds.put(name, new KindImpl(name));
	}

	public Kind getKind(final String kind) {
		return kinds.get(kind);
	}

	public void bindKind(final Kind oldKind, final String newKindName) throws DataException {
		assert (oldKind != null): "Supplied old kind is null.";
		assert (newKindName != null): "Supplied new kind name is null.";
		final String oldKindName = oldKind.getName();
		if (!kinds.containsKey(oldKindName)) {
			logger.error("Unable to bind kind: old kind not found: " + oldKindName);
			throw new DataException("Kind not found", oldKindName);
		}
		if (kinds.containsKey(newKindName)) {
			logger.error("Unable to bind kind: new kind already exists: " + newKindName);
			throw new DataException("Kind already exists", newKindName);
		}
		kinds.put(newKindName, kinds.get(oldKindName));
	}

	/**
	 * Binds two previously imported or exported kinds together.
	 * This method is used during import or export.
	 *
	 * @param kind1Name name of the first kind to bind together (must not be <code>null</code>).
	 * @param kind2Name name of the second kind to bind together (must not be <code>null</code>, must not be equal to <code>kind1Name</code>).
	 *
	 * @throws DataException if no kinds with <code>kind1Name</code> and <code>kind2Name</code> exist.
	 */
	void bindKind(final String kind1Name, final String kind2Name) throws DataException {
		assert (kind1Name != null): "Supplied first kind name is null.";
		assert (kind2Name != null): "Supplied second kind name is null.";
		assert (!kind1Name.equals(kind2Name)): "Supplied kind names are equal.";
		if (!(kinds.containsKey(kind1Name))) {
			logger.error("Unable to bind kind: kind " + kind1Name + " not found.");
			throw new DataException("Kind not found", kind1Name);
		}
		if (!(kinds.containsKey(kind2Name))) {
			logger.error("Unable to bind kind: kind " + kind2Name + " not found.");
			throw new DataException("Kind not found", kind2Name);
		}
		final Kind newKind = new KindImpl(kinds.get(kind1Name), kinds.get(kind2Name));
		kinds.put(kind1Name, newKind);
		kinds.put(kind2Name, newKind);
	}

	/**
	 * Defines a new {@link Functor}.
	 *
	 * @param name name of the new Functor (must not be <code>null</code>).
	 * @param resultKind resulting kind of the Functor (must not be <code>null</code>).
	 * @param inputKindList list of input kinds for the new Functor (must not be <code>null</code>).
	 *
	 * @throws DataException if a functor with the specified name already exists.
	 */
	// FIXME
	//void defineTerm(final String name, final Kind resultKind, final List<Kind> inputKindList) throws DataException {
	//	assert (name != null): "Supplied name is null.";
	//	assert (resultKind != null): "Supplied result kind is null.";
	//	assert (inputKindList != null): "Supplied input kind list is null.";
	//	if (terms.containsKey(name)) {
	//		logger.error("A term with name " + name + " already exists.");
	//		throw new DataException("Term already exists", name);
	//	}
	//	terms.put(name, new Functor(name, resultKind, inputKindList));
	//}

	public ComplexTerm getTerm(final String name) {
		return terms.get(name);
	}

	public @Override String toString() {
		return	  "Kinds: " + kinds.toString() + "; "
			+ "Terms: " + terms.toString() + "; "
			+ "Symbols: " + symbols.toString() + "; "
			+ "Parameters: " + parameters.toString();
	}

}
