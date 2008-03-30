package jhilbert.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import jhilbert.commands.Command;
import jhilbert.data.ModuleData;
import jhilbert.data.Statement;
import jhilbert.data.TermExpression;
import jhilbert.data.Token;
import jhilbert.data.Variable;
import jhilbert.exceptions.DataException;
import jhilbert.exceptions.ScannerException;
import jhilbert.exceptions.SyntaxException;
import jhilbert.exceptions.VerifyException;
import jhilbert.util.TokenScanner;

/**
 * Command introducing a new theorem or statement.
 * <p>
 * The format of the command is:
 * <br>
 * statementName ((var11 &hellip; var1N) &hellip; (varM1 &hellip; varMN)) (hypotheses) {@link TermExpression} [proof]
 * <p>
 * The format of the hypotheses depends on the command type. A proof is only present in {@link TheoremCommand}s.
 *
 * @see StatementCommand
 * @see TheoremCommand
 */
public abstract class AbstractStatementCommand extends Command {

	/**
	 * Distinct variable constraints (as a list of a list of strings).
	 */
	protected final List<List<String>> rawDVList;

	/**
	 * Hypotheses.
	 */
	protected final List<TermExpression> hypotheses;

	/**
	 * Consequent.
	 */
	protected final TermExpression consequent;

	/**
	 * Cooked statement.
	 */
	protected Statement statement;

	/**
	 * Scans the hypotheses from a TokenScanner.
	 * This method must be overridden by subclasses of AbstractStatementCommand.
	 * This method must fill the {@link #hypotheses}.
	 *
	 * @param tokenScanner TokenScanner to scan from.
	 * @param data ModuleData.
	 *
	 * @throws SyntaxException if a syntax error occurs.
	 * @throws ScannerException if a problem scanning the hypotheses occurs.
	 * @throws DataException if the hypotheses fail to be valid {@link TermExpression}s.
	 */
	protected abstract void scanHypotheses(final TokenScanner tokenScanner, final ModuleData data) throws SyntaxException, ScannerException, DataException;

	/**
	 * Scans a new statement from a TokenScanner.
	 * Does not scan proof if present.
	 * The parameters must not be <code>null</code>.
	 *
	 * @param commandName name of command.
	 * @param tokenScanner TokenScanner to scan from.
	 * @param data ModuleData.
	 *
	 * @throws SyntaxException if a syntax error occurs.
	 */
	protected AbstractStatementCommand(final String commandName, final TokenScanner tokenScanner, final ModuleData data) throws SyntaxException {
		super(data);
		assert (commandName != null): "Supplied command name is null.";
		assert (tokenScanner != null): "Supplied token scanner is null.";
		StringBuilder context = new StringBuilder(commandName);
		context.append(' ');
		try {
			name = tokenScanner.getAtom();
			context.append(name).append(' ');
			tokenScanner.beginExp();
			context.append('(');
			rawDVList = new ArrayList();
			Token outer = tokenScanner.getToken();
			while (outer.tokenClass == Token.TokenClass.BEGIN_EXP) {
				context.append('(');
				List<String> dvList = new ArrayList();
				Token inner = tokenScanner.getToken();
				while (inner.tokenClass == Token.TokenClass.ATOM) {
					final String atom = inner.toString();
					context.append(atom).append(' ');
					dvList.add(atom);
					inner = tokenScanner.getToken();
				}
				final int length = context.length();
				context.delete(length - 1, length);
				context.append(") ");
				if (inner.tokenClass != Token.TokenClass.END_EXP)
					throw new SyntaxException("Expected \")\"", context.toString());
				rawDVList.add(dvList);
				outer = tokenScanner.getToken();
			}
			final int length = context.length();
			context.delete(length - 1, length);
			context.append(')');
			if (outer.tokenClass != Token.TokenClass.END_EXP)
				throw new SyntaxException("Expected \")\"", context.toString());
			tokenScanner.beginExp();
			hypotheses = new ArrayList();
			scanHypotheses(tokenScanner, data);
			tokenScanner.endExp();
			context.append(" [hypotheses] ");
			consequent = new TermExpression(tokenScanner, data);
		} catch (NullPointerException e) {
			throw new SyntaxException("Unexpected end of input", context.toString(), e);
		} catch (DataException e) {
			throw new SyntaxException("Error scanning term", context.toString(), e);
		} catch (ScannerException e) {
			throw new SyntaxException("Scanner error", context.toString(), e);
		}
	}

	/**
	 * Partially executes this command.
	 * Subclasses of AbstractStatementCommand must override this method.
	 * This method creates the {@link #statement}.
	 *
	 * @throws VerifyException if the command cannot be executed.
	 */
	public @Override void execute() throws VerifyException {
		final List<SortedSet<Variable>> cookedDVList = new ArrayList();
		for (List<String> rawDV: rawDVList) {
			SortedSet<Variable> cookedDV = new TreeSet();
			for (String varName: rawDV) {
				if (!data.containsVariable(varName))
					throw new VerifyException("Constraint variable not defined", varName);
				if (!cookedDV.add((Variable) data.getLocalSymbol(varName)))
					throw new VerifyException("Constraint variable occurring twice", varName);
			}
			cookedDVList.add(cookedDV);
		}
		statement = new Statement(data.getPrefix() + name, cookedDVList, hypotheses, consequent);
	}

}
