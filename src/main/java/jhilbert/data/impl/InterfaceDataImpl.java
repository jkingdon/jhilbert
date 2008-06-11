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

import java.io.EOFException;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import jhilbert.data.Data;
import jhilbert.data.Kind;
import jhilbert.data.InterfaceData;
import jhilbert.data.Parameter;
import jhilbert.data.Statement;
import jhilbert.data.Symbol;
import jhilbert.data.TermExpression;
import jhilbert.data.Variable;
import jhilbert.data.VariablePair;
import jhilbert.data.impl.DataFactoryImpl;
import jhilbert.data.impl.DataImpl;
import jhilbert.data.impl.KindImpl;
import jhilbert.data.impl.ParameterizedName;
import jhilbert.exceptions.DataException;
import jhilbert.exceptions.InputException;
import jhilbert.exceptions.UnknownFormatException;
import jhilbert.util.DataInputStream;
import jhilbert.util.DataOutputStream;
import org.apache.log4j.Logger;

/**
 * Data collected in an {@link Interface}.
 * <p>
 * The parameters to the methods and constructors of this class must never be <code>null</code>.
 * Otherwise, the behavior of the whole class and its objects is undefined.
 * <p>
 * This class provides the {@link #store()} method to byte-compile the interface data.
 * <p>
 * This class uses permissive querying: unknown kinds and terms are created on demand and internally marked undefined.
 */
final class InterfaceDataImpl extends DataImpl implements InterfaceData {

	/**
	 * Logger for this class.
	 */
	private final Logger logger = Logger.getLogger(InterfaceDataImpl.class);

	/**
	 * Undefined kinds names.
	 */
	private final Map<String, ParameterizedName> undefinedKindNames;

	/**
	 * Kinds.
	 */
	private final LinkedHashMap<String, KindImpl> kinds;

	/**
	 * Undefined term names.
	 */
	private final Map<String, ParameterizedName> undefinedTermNames;

	/**
	 * Creates new empty interface data.
	 */
	public InterfaceDataImpl() {
		super();
		undefinedKindNames = new HashMap();
		kinds = new LinkedHashMap(); // for sound insertion order
		undefinedTermNames = new HashMap();
		terms = new LinkedHashMap(); // for sound insertion order
	}

	/**
	 * Loads previously stored interface data from the specified input stream.
	 *
	 * @param in input stream.
	 *
	 * @throws UnknownFormatException if the {@link DataImpl#FORMAT_VERSION} is incorrect.
	 * @throws DataException if any other error occurs while loading the data.
	 *
	 * @see #store()
	 */
	public InterfaceDataImpl(final InputStream in) throws UnknownFormatException, DataException {
		assert (in != null): "Supplied input stream is null.";
		final DataInputStream ds = new DataInputStream(in);
		undefinedKindNames = new HashMap();
		kinds = new LinkedHashMap();
		undefinedTermNames = new HashMap();
		terms = new HashMap();
		try {
			final int format = ds.readInt();
			if (format != DataImpl.FORMAT_VERSION) {
				logger.error("Unknown interface data file format version.");
				logger.error("Got version: " + format);
				logger.error("Expected version: " + DataImpl.FORMAT_VERSION);
				throw new UnknownFormatException("Unknown interface data file format version",
					in.toString());
			}
			// read data sizes
			final int parameterSize = ds.readNonNegativeInt();
			final int undefinedKindsSize = ds.readNonNegativeInt();
			final int undefinedTermNamesSize = ds.readNonNegativeInt();
			final int definedKindsSize = ds.readNonNegativeInt();
			final int definedTermNamesSize = ds.readNonNegativeInt();
			final int statementsSize = ds.readNonNegativeInt();
			// calculate bounds
			final int parameterLowerBound = 1;
			final int parameterUpperBound = parameterLowerBound + parameterSize;
			final int kindsLowerBound = parameterUpperBound;
			final int undefinedKindsLowerBound = kindsLowerBound;
			final int undefinedKindsUpperBound = undefinedKindsLowerBound + undefinedKindsSize;
			final int definedKindsLowerBound = undefinedKindsUpperBound;
			final int definedKindsUpperBound = definedKindsLowerBound + definedKindsSize;
			final int kindsUpperBound = definedKindsUpperBound;
			final int termNamesLowerBound = definedKindsUpperBound;
			final int undefinedTermNamesLowerBound = termNamesLowerBound;
			final int undefinedTermNamesUpperBound = undefinedTermNamesLowerBound + undefinedTermNamesSize;
			final int definedTermNamesLowerBound = undefinedTermNamesUpperBound;
			final int definedTermNamesUpperBound = definedTermNamesLowerBound + definedTermNamesSize;
			final int termNamesUpperBound = definedTermNamesUpperBound;
			final int statementsLowerBound = definedTermNamesUpperBound;
			final int statementsUpperBound = statementsLowerBound + statementsSize;
			final int totalSize = statementsUpperBound;
			// read name list
			final List<String> nameList = new ArrayList(totalSize);
			nameList.add(null); // for unknown kinds, see ComplexTerm#store()
			for (int i = 1; i != totalSize; ++i)
				nameList.add(ds.readString());
			// load parameters
			for (int i = parameterLowerBound; i != parameterUpperBound; ++i) {
				final String parameterName = nameList.get(i);
				parameters.put(parameterName, new ParameterImpl(parameterName, ds, this, nameList,
									parameterLowerBound, parameterUpperBound));
			}
			// load undefined kinds
			for (int i = undefinedKindsLowerBound; i != undefinedKindsUpperBound; ++i) {
				final String kindName = nameList.get(i);
				final ParameterImpl parameter = parameters.get(nameList
					.get(ds.readInt(parameterLowerBound, parameterUpperBound)));
				final String fullName = parameter.getPrefix() + kindName;
				if (undefinedKindNames.containsKey(fullName)) {
					logger.error("Undefined kind " + fullName + " specified twice.");
					throw new DataException("Undefined kind specified twice", fullName);
				}
				undefinedKindNames.put(fullName, new ParameterizedName(parameter, kindName));
				kinds.put(fullName, new KindImpl(fullName));
			}
			// load defined kinds
			for (int i = definedKindsLowerBound; i != definedKindsUpperBound; ++i) {
				final String kindName = nameList.get(i);
				if (kinds.containsKey(kindName)) {
					logger.error("Kind " + kindName + " specified twice.");
					throw new DataException("Kind specified twice", kindName);
				}
				final String targetKindName = nameList.get(ds.readInt(kindsLowerBound, kindsUpperBound));
				final KindImpl newKind = new KindImpl(kindName);
				// make sure only one new kind is created for every kind name
				kinds.put(kindName, newKind);
				KindImpl targetKind = kinds.get(targetKindName);
				if (targetKind == null)
					targetKind = new KindImpl(targetKindName);
				kinds.put(kindName, targetKind);
			}
			// load undefined terms
			for (int i = undefinedTermNamesLowerBound; i != undefinedTermNamesUpperBound; ++i) {
				final String termName = nameList.get(i);
				final ParameterImpl parameter = parameters.get(nameList
					.get(ds.readInt(parameterLowerBound, parameterUpperBound)));
				final String fullName = parameter.getPrefix() + termName;
				if (undefinedTermNames.containsKey(fullName)) {
					logger.error("Undefined term " + fullName + " specified twice.");
					throw new DataException("Undefined term specified twice", fullName);
				}
				undefinedTermNames.put(fullName, new ParameterizedName(parameter, termName));
				terms.put(fullName, ComplexTerm.create(fullName, ds, this, nameList,
					kindsLowerBound, kindsUpperBound));
			}
			// load defined terms
			for (int i = definedTermNamesLowerBound; i != definedTermNamesUpperBound; ++i) {
				final String termName = nameList.get(i);
				if (terms.containsKey(termName)) {
					logger.error("Defined term " + termName + " specified twice.");
					throw new DataException("Defined term specified twice", termName);
				}
				terms.put(termName, ComplexTerm.create(termName, ds, this, nameList,
					kindsLowerBound, kindsUpperBound, termNamesLowerBound, i));
			}
			// load statements
			for (int i = statementsLowerBound; i != statementsUpperBound; ++i) {
				final String statementName = nameList.get(i);
				if (symbols.containsKey(statementName)) {
					logger.error("Statement " + statementName + " specified twice.");
					throw new DataException("Statement specified twice", statementName);
				}
				symbols.put(statementName, new StatementImpl(statementName, ds, this, nameList, kindsLowerBound,
					kindsUpperBound, termNamesLowerBound, termNamesUpperBound));
			}
			if (ds.read() != -1) {
				logger.error("Interface completely loaded but input stream " + ds + "is not at EOF");
				throw new DataException("Interface completely loaded but input stream is not at EOF",
						ds.toString());
			}
		} catch (EOFException e) {
			logger.error("Unexpected end of input while loading interface.", e);
			throw new DataException("Unexpected end of input while loading interface.", ds.toString(), e);
		} catch (IOException e) {
			logger.error("I/O error while loading interface.", e);
			throw new DataException("I/O error while loading interface.", ds.toString(), e);
		}
	}

	public @Override KindImpl getKind(final String name) throws DataException {
		assert (name != null): "Supplied kind name is null.";
		KindImpl result = kinds.get(name);
		if (result == null) {
			// search kind
			final DataFactoryImpl df = DataFactoryImpl.getInstance();
			for (final Map.Entry<String, ParameterImpl> paramMapping: parameters.entrySet()) {
				final ParameterImpl parameter = paramMapping.getValue();
				final String prefix = parameter.getPrefix();
				if (name.startsWith(prefix))
					try {
						final String bareName = name.substring(prefix.length());
						final InterfaceDataImpl data = df.getInterfaceData(parameter.getLocator());
						if (data.undefinedKindNames.containsKey(bareName))
							continue;
						if (data.kinds.containsKey(bareName)) {
							undefinedKindNames.put(name,
								new ParameterizedName(parameter, bareName));
							result = new KindImpl(name);
							kinds.put(name, result);
							return result;
						}
					} catch (InputException e) {
						logger.error("Unable to obtain interface " + parameter);
						throw new DataException("Unable to obtain interface",
							parameter.toString(), e);
					}
			}
			// nothing found, eh?
			return null;
		}
		return result;
	}

	public void defineKind(final String name) throws DataException {
		assert (name != null): "Supplied kind name is null.";
		if (kinds.containsKey(name)) {
			logger.error("Kind " + name + " already defined or referenced.");
			throw new DataException("Kind already defined or referenced", name);
		}
		kinds.put(name, new KindImpl(name));
	}

	public @Override void bindKind(final Kind oldKind, final String newKindName) throws DataException {
		assert (oldKind != null): "Supplied existing kind is null.";
		assert (newKindName != null): "Supplied name of new kind is null.";
		final String oldKindName = oldKind.toString();
		if (oldKindName.equals(newKindName)) {
			logger.error("Binding kind " + newKindName + ": old and new kind are identical.");
			throw new DataException("Old and new kind are identical while binding kind.", newKindName);
		}
		if (kinds.containsKey(newKindName)) {
			logger.error("Binding kind: " + newKindName + " already defined or referenced.");
			throw new DataException("Kind already defined or referenced while binding kind.", newKindName);
		}
		kinds.put(newKindName, getKind(oldKindName));
	}

	public @Override ComplexTerm getTerm(final String name) throws DataException {
		assert (name != null): "Supplied term name is null.";
		ComplexTerm result = terms.get(name);
		if (result == null) {
			// search term
			final DataFactoryImpl df = DataFactoryImpl.getInstance();
			for (final Map.Entry<String, ParameterImpl> paramMapping: parameters.entrySet()) {
				final ParameterImpl parameter = paramMapping.getValue();
				final String prefix = parameter.getPrefix();
				if (name.startsWith(prefix))
					try {
						final String bareName = name.substring(prefix.length());
						final InterfaceDataImpl data = df.getInterfaceData(parameter.getLocator());
						if (data.undefinedTermNames.containsKey(bareName))
							continue;
						if (data.terms.containsKey(bareName)) {
							undefinedTermNames.put(name,
								new ParameterizedName(parameter, bareName));
							result = new Functor(name);
							terms.put(name, result);
							return result;
						}
					} catch (InputException e) {
						logger.error("Unable to obtain interface " + parameter);
						throw new DataException("Unable to obtain interface",
							parameter.toString(), e);
					}
			}
			// nothing found? Then it's straight from the module
			undefinedTermNames.put(name, new ParameterizedName(ParameterImpl.MAIN_PARAMETER, name));
			result = new Functor(name);
			terms.put(name, result);
		}
		return result;
	}

	public void defineTerm(final String name, final Kind resultKind, final List<Kind> inputKindList)
	throws DataException {
		assert (name != null): "Supplied term name is null.";
		assert (resultKind != null): "Supplied result kind is null.";
		assert (inputKindList != null): "Supplied list of input kinds is null.";
		if (terms.containsKey(name)) {
			logger.error("Term " + name + " already defined or referenced.");
			throw new DataException("Term already defined or referenced.", name);
		}
		terms.put(name, new Functor(name, resultKind, inputKindList));
	}

	/**
	 * Stores interface data in the specified output stream.
	 *
	 * @param out OutputStream.
	 *
	 * @throws DataException if an I/O error occurs.
	 */
	public void store(final OutputStream out) throws DataException {
		final Map<String, Integer> parameterNameTable = new HashMap();
		final Map<String, Integer> kindNameTable = new HashMap();
		final Map<String, Integer> termNameTable = new HashMap();
		int currentID = 1;
		final Set<String> definedKinds = new HashSet(kinds.keySet());
		definedKinds.removeAll(undefinedKindNames.keySet());
		final Set<String> definedTermNames = new HashSet(terms.keySet());
		definedTermNames.removeAll(undefinedTermNames.keySet());
		final Map<String, StatementImpl> statements = new HashMap();
		for (Symbol symbol: symbols.values())
			if (!symbol.isVariable())
				statements.put(symbol.toString(), (StatementImpl) symbol);
		try {
			final DataOutputStream ds = new DataOutputStream(out);
			// structure sizes
			ds.writeInt(DataImpl.FORMAT_VERSION);	// 0: version
			ds.writeInt(parameters.size());		// 4: number of parameters
			ds.writeInt(undefinedKindNames.size());	// 8: # of undefined kinds
			ds.writeInt(undefinedTermNames.size());	// 12: # of undefined terms
			ds.writeInt(definedKinds.size());	// 16: # of defined kinds
			ds.writeInt(definedTermNames.size());	// 20: # of defined terms
			ds.writeInt(statements.size());		// 24: # of statements
			// name list
			for (final String parameterName: parameters.keySet()) {
				parameterNameTable.put(parameterName, currentID++);
				ds.writeString(parameterName);
			}
			for (final Map.Entry<String, ParameterizedName> undefinedKindNameMapping:
					undefinedKindNames.entrySet()) {
				kindNameTable.put(undefinedKindNameMapping.getKey(), currentID++);
				ds.writeString(undefinedKindNameMapping.getValue().getName());
			}
			for (final Map.Entry<String, ParameterizedName> undefinedTermNameMapping:
					undefinedTermNames.entrySet()) {
				termNameTable.put(undefinedTermNameMapping.getKey(), currentID++);
				ds.writeString(undefinedTermNameMapping.getValue().getName());
			}
			for (final String definedKind: definedKinds) {
				kindNameTable.put(definedKind, currentID++);
				ds.writeString(definedKind);
			}
			for (final String definedTermName: definedTermNames) {
				termNameTable.put(definedTermName, currentID++);
				ds.writeString(definedTermName);
			}
			for (final String statementName: statements.keySet())
				ds.writeString(statementName);
			// parameters
			for (final String parameterName: parameters.keySet())
				parameters.get(parameterName).store(ds, parameterNameTable);
			// undefined kinds
			for (final Map.Entry<String, ParameterizedName> mapping: undefinedKindNames.entrySet())
				ds.writeInt(parameterNameTable.get(mapping.getValue().getInterfaceName()));
			// kind mappings (defined kinds only)
			for (final String definedKind: definedKinds)
				ds.writeInt(kindNameTable.get(kinds.get(definedKind).toString()));
			// undefined terms (partial info)
			for (final Map.Entry<String, ParameterizedName> mapping: undefinedTermNames.entrySet()) {
				ds.writeInt(parameterNameTable.get(mapping.getValue().getInterfaceName()));
				terms.get(mapping.getKey()).store(ds, kindNameTable, termNameTable);
			}
			// defined terms
			for (String definedTermName: definedTermNames)
				terms.get(definedTermName).store(ds, kindNameTable, termNameTable);
			// statements
			for (String statementName: statements.keySet())
				statements.get(statementName).store(ds, kindNameTable, termNameTable);
			ds.flush();
		} catch (IOException e) {
			logger.error("I/O error while writing interface data to " + out, e);
			throw new DataException("I/O error while writing interface data", out.toString(), e);
		}
	}

	/**
	 * Imports these interface data into the specified module data, defining the specified parameter.
	 *
	 * @param moduleData module data to import these data into (must not be <code>null</code>).
	 * @param parameter parameter to define (must not be <code>null</code>).
	 *
	 * @throws DataException if the import fails.
	 */
	void importInto(final ModuleDataImpl moduleData, final ParameterImpl parameter) throws DataException {
		assert (moduleData != null): "Supplied module data are null.";
		assert (parameter != null): "Supplied parameter is null.";
		final Map<Variable, Variable> varMap = new HashMap();
		// parameters
		final Map<String, ParameterImpl> parameterMap = checkParameters(moduleData, parameter);
		setParameters(parameterMap);
		final String prefix = parameter.getPrefix();
		final Map<String, String> kindNameMap = new HashMap();
		final Map<String, String> termNameMap = new HashMap();
		// kinds
		final Map<String, String> aliasMap = new LinkedHashMap();
		for (final Map.Entry<String, KindImpl> kindMapping: kinds.entrySet()) {
			final String key = kindMapping.getKey();
			final String value = kindMapping.getValue().toString();
			if (undefinedKindNames.containsKey(key)) {
				final ParameterizedName pn = undefinedKindNames.get(key);
				final String fullName = pn.getModulePrefix() + pn.getName();
				if (moduleData.getKind(fullName) == null) {
					logger.error("Import: unable to link kind " + key + " to " + fullName);
					throw new DataException("Unable to link kind", key);
				}
				kindNameMap.put(key, fullName);
			} else if (key.equals(value)) {
				final String fullName = prefix + key;
				moduleData.defineKind(fullName);
				kindNameMap.put(key, fullName);
			} else
				aliasMap.put(key, value);
		}
		for (final Map.Entry<String, String> aliasMapping: aliasMap.entrySet()) {
			final String key = aliasMapping.getKey();
			final String fullName = prefix + key;
			moduleData.bindKind(moduleData.getKind(kindNameMap.get(aliasMapping.getValue())),
				fullName);
			kindNameMap.put(key, fullName);
		}
		// terms
		for (final Map.Entry<String, ComplexTerm> termMapping: terms.entrySet()) {
			final String key = termMapping.getKey();
			final ComplexTerm value = termMapping.getValue();
			if (undefinedTermNames.containsKey(key)) {
				final ParameterizedName pn = undefinedTermNames.get(key);
				final String fullName = pn.getModulePrefix() + pn.getName();
				if (moduleData.getTerm(fullName) == null) {
					logger.error("Import: unable to link term " + key + " to " + fullName);
					throw new DataException("Unable to link term", key);
				}
				termNameMap.put(key, fullName);
			} else if (value instanceof Functor) {
				final Functor functor = (Functor) value;
				final Kind resultKind = moduleData.getKind(kindNameMap.get(functor.getKind().toString()));
				assert (resultKind != null): "Result kind null during Functor import.";
				final int placeCount = functor.placeCount();
				assert (placeCount >= 0): "Place count negative during Functor import.";
				final List<Kind> inputKinds = new ArrayList(placeCount);
				for (int i = 0; i != placeCount; ++i) {
					final Kind inputKind = moduleData.getKind(kindNameMap
						.get(functor.getInputKind(i).toString()));
					assert (inputKind != null): "Input kind null during Functor import.";
					inputKinds.add(inputKind);
				}
				final String fullName = prefix + key;
				moduleData.defineTerm(fullName, resultKind, inputKinds);
				termNameMap.put(key, fullName);
			} else if (value instanceof Definition) {
				final Definition definition = (Definition) value;
				final TermExpressionImpl definiens
					= definition.getDefiniens().adapt(moduleData, kindNameMap, termNameMap, varMap);
				final LinkedHashSet<Variable> varList = definiens.variables();
				// throw out dummies
				// FIXME: removed: definiens should not contain dummies in folded state
				// final Iterator<Variable> i = varList.iterator();
				// while (i.hasNext()) {
				//	final Variable var = i.next();
				//	if (var instanceof DummyVariable)
				//		i.remove();
				//}
				assert (varList.size() == definition.placeCount()):
					"Place count mismatch during Definition import.";
				final String fullName = prefix + key;
				moduleData.defineTerm(fullName, varList, definiens);
				termNameMap.put(key, fullName);
			} else
				assert false: "Term not from this implementation.";
		}
		// symbols
		for (final Symbol symbol: symbols.values()) {
			if (symbol.isVariable())
				continue;
			assert (symbol instanceof StatementImpl): "Symbol not from this implementation.";
			final StatementImpl statement = (StatementImpl) symbol;
			final List<SortedSet<Variable>> rawDV
				= statement.getDVConstraints().adapt(moduleData, kindNameMap, varMap);
			final List<TermExpression> hypotheses = new ArrayList();
			for (final TermExpression hypothesis: statement.getHypotheses()) {
				assert (hypothesis instanceof TermExpressionImpl):
					"Hypothesis not from this implementation.";
				hypotheses.add(((TermExpressionImpl) hypothesis)
					.adapt(moduleData, kindNameMap, termNameMap, varMap));
			}
			final TermExpressionImpl consequent = statement.getConsequent()
				.adapt(moduleData, kindNameMap, termNameMap, varMap);
			moduleData.defineStatement(statement.toString(), rawDV, hypotheses, consequent);
		}
		unsetParameters();
	}

	/**
	 * Exports these interface data from the specified module data using the specified parameter.
	 *
	 * @param moduleData module data to export these data from (must not be <code>null</code>).
	 * @param parameter parameter to use (must not be <code>null</code>).
	 *
	 * @throws DataException if the export fails.
	 */
	void exportFrom(final ModuleDataImpl moduleData, final ParameterImpl parameter) throws DataException {
		assert (moduleData != null): "Supplied module dara are null.";
		assert (parameter != null): "Supplied parameter is null.";
		final Map<String, ParameterImpl> parameterMap = checkParameters(moduleData, parameter);
		setParameters(parameterMap);
		final String prefix = parameter.getPrefix();
		final Map<String, String> kindNameMap = new HashMap();
		final Map<String, String> termNameMap = new HashMap();
		// kinds
		for (final String kindName: kinds.keySet()) {
			String moduleName;
			if (undefinedKindNames.containsKey(kindName)) {
				final ParameterizedName pn = undefinedKindNames.get(kindName);
				moduleName = pn.getModulePrefix() + pn.getName();
			} else
				moduleName = prefix + kindName;
			if (moduleData.getKind(moduleName) == null) {
				logger.error("Kind " + kindName + " unknown during export.");
				throw new DataException("Kind unknown during export", kindName);
			}
			kindNameMap.put(kindName, moduleName);
		}
		// terms
		for (final ComplexTerm term: terms.values()) {
			final String key = term.toString();
			String moduleName = key;
			if (undefinedTermNames.containsKey(moduleName)) {
				final ParameterizedName pn = undefinedTermNames.get(moduleName);
				moduleName = pn.getModulePrefix() + pn.getName();
			} else
				moduleName = prefix + moduleName;
			final ComplexTerm moduleTerm = moduleData.getTerm(moduleName);
			if (moduleTerm == null) {
				logger.error("Term " + term + " unknown during export.");
				throw new DataException("Term unknown during export", term.toString());
			}
			// result kind
			final Kind resultKind = term.getKind();
			if (resultKind != null) {
				if (!moduleTerm.getKind()
						.equals(moduleData.getKind(kindNameMap.get(resultKind.toString())))) {
					logger.error("Result kind mismatch during export of term " + term);
					logger.error("Needed result kind: "
						+ moduleData.getKind(kindNameMap.get(resultKind.toString())));
					logger.error("Found result kind:  " + moduleTerm.getKind());
					throw new DataException("Result kind mismatch during term export", term.toString());
				}
			} else
				assert (undefinedTermNames.containsKey(term.toString())): "Unknown result kind for known term.";
			// place count
			final int placeCount = term.placeCount();
			assert (placeCount >= 0): "Negative place count in term during export.";
			if (placeCount != moduleTerm.placeCount()) {
				logger.error("Place count mismatch during export of term " + term);
				logger.error("Needed place count: " + placeCount);
				logger.error("Found place count:  " + moduleTerm.placeCount());
				throw new DataException("Place count mismatch during term export", term.toString());
			}
			// input kinds
			for (int i = 0; i != placeCount; ++i) {
				final Kind inputKind = term.getInputKind(i);
				if (inputKind != null) {
					if (!moduleTerm.getInputKind(i)
							.equals(moduleData
							.getKind(kindNameMap.get(inputKind.toString())))) {
						logger.error("Input kind mismatch during export of term " + term);
						logger.error("Position: " + (i+1));
						logger.error("Needed input kind: "
							+ moduleData.getKind(kindNameMap.get(inputKind.toString())));
						logger.error("Found input kind:  " + moduleTerm.getInputKind(i));
						throw new DataException("Input kind mismatch during term export", term.toString());
					}
				} else
					assert (undefinedTermNames.containsKey(key)): "Unknown input kind for known term.";
			}
			termNameMap.put(key, moduleName);
		}
		// symbols
		for (final Symbol symbol: symbols.values()) {
			if (symbol.isVariable())
				continue;
			assert (symbol instanceof StatementImpl): "Statement not from this implementation.";
			final StatementImpl statement = (StatementImpl) symbol;
			final StatementImpl moduleStatement = moduleData.getStatement(prefix + statement.toString());
			if (moduleStatement == null) {
				logger.error("Statement " + statement + " unknown during export.");
				throw new DataException("Statement unknown during export", statement.toString());
			}
			// adapt expressions
			final Map<Variable, Variable> varMap = new HashMap();
			final List<TermExpressionImpl> hypotheses = new ArrayList();
			for (final TermExpression hypothesis: statement.getHypotheses()) {
				assert (hypothesis instanceof TermExpressionImpl): "hypothesis not from this implementation.";
				hypotheses.add(((TermExpressionImpl) hypothesis)
					.adapt(moduleData, kindNameMap, termNameMap, varMap));
			}
			final TermExpressionImpl consequent = statement.getConsequent()
				.adapt(moduleData, kindNameMap, termNameMap, varMap);
			// compare expressions
			final Map<Variable, Variable> translationMap = new HashMap();
			final List<TermExpression> moduleHypotheses = moduleStatement.getHypotheses();
			final int numHyps = hypotheses.size();
			if (numHyps != moduleHypotheses.size()) {
				logger.error("Hypothesis count mismatch in statement " + statement + " during export.");
				logger.error("Expected # of hypotheses: " + moduleHypotheses.size());
				logger.error("Received # of hypotheses: " + numHyps);
				throw new DataException("Hypothesis count mismatch during export", statement.toString());
			}
			for (int i = 0; i != numHyps; ++i) {
				final TermExpression moduleHypothesis = moduleHypotheses.get(i);
				assert (moduleHypothesis instanceof TermExpressionImpl): "Module hypothesis not from this implementation.";
				hypotheses.get(i).equalityMap((TermExpressionImpl) moduleHypothesis, translationMap);
			}
			consequent.equalityMap(moduleStatement.getConsequent(), translationMap);
			// check DV constraints
			final DVConstraintsImpl moduleDV = new DVConstraintsImpl(moduleStatement.getDVConstraints());
			final List<SortedSet<Variable>> rawDVConstraints = statement.getDVConstraints().adapt(moduleData, kindNameMap, varMap);
			final DVConstraintsImpl dvConstraints = new DVConstraintsImpl();
			for (final SortedSet<Variable> rawDVConstraint: rawDVConstraints)
				dvConstraints.add(rawDVConstraint);
			for (final VariablePair p: dvConstraints) {
				if (!moduleDV.remove(new VariablePairImpl(translationMap.get(p.getFirst()), translationMap.get(p.getSecond())))) {
					logger.error("Missing DV constraints in target of " + statement + " during export.");
					logger.error("Offending constraint: " + p);
					throw new DataException("Missing DV constraints in export target", statement.toString());
				}
			}
			if (!moduleDV.isEmpty()) {
				logger.error("Superfluous DV constraints in target of " + statement + " during export.");
				logger.error("Superfluous constraints: " + moduleDV);
				throw new DataException("Superfluous DV constraints during export", statement.toString());
			}
		}
		unsetParameters();
	}

	/**
	 * Sets the module parameters of the undefined names.
	 */
	private void setParameters(final Map<String, ParameterImpl> parameterMap) {
		for (final ParameterizedName pn: undefinedKindNames.values()) {
			final String name = pn.getInterfaceName();
			if (parameterMap.containsKey(name))
				pn.setModuleParameter(parameterMap.get(name));
			else
				pn.setModuleParameter(); // FIXME: for null parameters (currently not implemented)
		}
		for (final ParameterizedName pn: undefinedTermNames.values()) {
			final String name = pn.getInterfaceName();
			if (parameterMap.containsKey(name))
				pn.setModuleParameter(parameterMap.get(name));
			else
				pn.setModuleParameter(); // for MAIN parameters
		}
	}

	/**
	 * Unsets the module parameters of the undefined names.
	 */
	private void unsetParameters() {
		for (final ParameterizedName pn: undefinedKindNames.values())
			pn.setModuleParameter(null);
		for (final ParameterizedName pn: undefinedTermNames.values())
			pn.setModuleParameter(null);
	}

	/**
	 * Checks whether the parameters provided by the module's import or export command
	 * satisfy the interface parameters specified by the param commands.
	 *
	 * @param moduleData the module data
	 * @param parameter the parameter these interface data should define, provided the import/export succeeds.
	 *
	 * @return a map mapping interface parameter names to their module parameters.
	 *
	 * @throws DataException if an argument does not verify, or if an interface cannot be loaded.
	 */
	private Map<String, ParameterImpl> checkParameters(final ModuleDataImpl moduleData,
		final ParameterImpl parameter)
	throws DataException {
		final List<Parameter> parameterList = parameter.getParameterList();
		final int paramCount = parameterList.size();
		final DataFactoryImpl df = DataFactoryImpl.getInstance();
		if (paramCount != parameters.size()) {
			logger.error("Wrong number of parameters while in interface " + parameter.toString());
			logger.error("Expected number of parameters: " + parameters.size());
			logger.error("Received number of parameters: " + paramCount);
			throw new DataException("Wrong number of parameters", parameter.toString());
		}
		final Map<String, ParameterImpl> result = new HashMap();
		int i = 0;
		for (final Map.Entry<String,ParameterImpl> ifParamMapping: parameters.entrySet()) {
			final ParameterImpl ifParam = ifParamMapping.getValue();
			final ParameterImpl argument = (ParameterImpl) parameterList.get(i);
			result.put(ifParamMapping.getKey(), argument);
			if (!(ifParam.getLocator().equals(argument.getLocator()) && ifParam.getPrefix().equals(argument.getPrefix()))) {
				logger.info("Checking whether parameter " + argument + " satisfies " + parameter);
				try {
					final InterfaceDataImpl ifdata = df.getInterfaceData(ifParam.getLocator());
					ifdata.exportFrom(moduleData, argument);
				} catch (DataException e) {
					logger.error("Parameter " + argument + " does not satisfy " + ifParam);
					throw new DataException("Parameter satisfaction error", argument.toString(), e);
				} catch (InputException e) {
					logger.error("Unable to load interface " + ifParam);
					throw new DataException("Unable to load interface", ifParam.toString(), e);
				}
			}
			++i;
		}
		return result;
	}

}
