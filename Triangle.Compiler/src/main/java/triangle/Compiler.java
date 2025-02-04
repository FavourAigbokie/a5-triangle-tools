/*
 * @(#)Compiler.java                       
 * 
 * Revisions and updates (c) 2022-2024 Sandy Brownlee. alexander.brownlee@stir.ac.uk
 * 
 * Original release:
 *
 * Copyright (C) 1999, 2003 D.A. Watt and D.F. Brown
 * Dept. of Computing Science, University of Glasgow, Glasgow G12 8QQ Scotland
 * and School of Computer and Math Sciences, The Robert Gordon University,
 * St. Andrew Street, Aberdeen AB25 1HG, Scotland.
 * All rights reserved.
 *
 * This software is provided free for educational use only. It may
 * not be used for commercial purposes without the prior written permission
 * of the authors.
 */

package triangle;

import picocli.CommandLine;
import triangle.abstractSyntaxTrees.Program;
import triangle.codeGenerator.Emitter;
import triangle.codeGenerator.Encoder;
import triangle.contextualAnalyzer.Checker;
import triangle.optimiser.ConstantFolder;
import triangle.optimiser.SummaryVisitor; // task 5.b
import triangle.syntacticAnalyzer.Parser;
import triangle.syntacticAnalyzer.Scanner;
import triangle.syntacticAnalyzer.SourceFile;
import triangle.treeDrawer.Drawer;
import triangle.abstractSyntaxTrees.Program;
/**
 * The main driver class for the Triangle compiler.
 *
 * @version 2.1 7 Oct 2003
 * @author Deryck F. Brown
 */
public class Compiler {



		/** CLI Arguments class for Picocli parsing */
		public static class CompilerArgs {
			@CommandLine.Parameters(index = "0", description = "The source filename to compile.")
			public String sourceName;

			@CommandLine.Option(names = "-o", description = "The output filename.", defaultValue = "obj.tam")
			public String objectName;

			@CommandLine.Option(names = "--showTree", description = "Show the abstract syntax tree.")
			public boolean showTree = false;

			@CommandLine.Option(names = "--folding", description = "Enable constant folding.")
			public boolean folding = false;

			@CommandLine.Option(names = "--showTreeAfter", description = "Show the abstract syntax tree after folding.")
			public boolean showTreeAfter = false;
            //task 5b adding options show stats
			@CommandLine.Option(names = "--showStats", description = "Summary statistics.")
			public boolean showStats = false;
		}
	private static Scanner scanner;
	private static Parser parser;
	private static Checker checker;
	private static Encoder encoder;
	private static Emitter emitter;
	private static ErrorReporter reporter;
	private static Drawer drawer;

	/** The AST representing the source program. */
	private static Program theAST;

	/**
	 * Compile the source program to TAM machine code.
	 *
	 * @param sourceName   the name of the file containing the source program.
	 * @param objectName   the name of the file containing the object program.
	 * @param showingAST   true iff the AST is to be displayed after contextual
	 *                     analysis
	 * @param showingTreeAfter true if the AST is to be displayed after constant folding.
	 * @param showingTable true iff the object description details are to be
	 *                     displayed during code generation (not currently
	 *                     implemented).
	 * @return true iff the source program is free of compile-time errors, otherwise
	 *         false.
	 */
	static boolean compileProgram(String sourceName, String objectName, boolean showingAST, boolean showingTreeAfter, boolean showStats, boolean showingTable) {

		System.out.println("********** " + "Triangle Compiler (Java Version 2.1)" + " **********");

		System.out.println("Syntactic Analysis ...");
		SourceFile source = SourceFile.ofPath(sourceName);

		if (source == null) {
			System.out.println("Can't access source file " + sourceName);
			System.exit(1);
		}

		scanner = new Scanner(source);
		reporter = new ErrorReporter(false);
		parser = new Parser(scanner, reporter);
		checker = new Checker(reporter);
		emitter = new Emitter(reporter);
		encoder = new Encoder(emitter, reporter);
		drawer = new Drawer();

		// scanner.enableDebugging();
		theAST = parser.parseProgram(); // 1st pass
		if (reporter.getNumErrors() == 0) {
			// if (showingAST) {
			// drawer.draw(theAST);
			// }
			System.out.println("Contextual Analysis ...");
			checker.check(theAST); // 2nd pass
			if (showingAST) {
				drawer.draw(theAST);
			}
			if (showingTreeAfter || showingAST) {
				theAST.visit(new ConstantFolder());
			}
			if (showingTreeAfter) {
				System.out.println("Abstract Syntax Tree after constant folding:");
				drawer.draw(theAST);
			}

			// Task 5.b display the summary statistics if passed in the arguments
			if(showStats) {
				SummaryVisitor summaryVisitor = new SummaryVisitor();
				theAST.visit(summaryVisitor);
				summaryVisitor.printSummaryStats();
			}
			
			if (reporter.getNumErrors() == 0) {
				System.out.println("Code Generation ...");
				encoder.encodeRun(theAST, showingTable); // 3rd pass
			}
		}

		boolean successful = (reporter.getNumErrors() == 0);
		if (successful) {
			emitter.saveObjectProgram(objectName);
			System.out.println("Compilation was successful.");
		} else {
			System.out.println("Compilation was unsuccessful.");
		}
		return successful;
	}

	/**
	 * Triangle compiler main program.
	 *
	 * @param args the only command-line argument to the program specifies the
	 *             source filename.
	 */
	public static void main(String[] args) {


		CompilerArgs compilerArgs = new CompilerArgs();
		CommandLine cmd = new CommandLine(compilerArgs);

		try {
			// Parse the arguments using Picocli
			cmd.parseArgs(args);

			// Run the compiler using parsed arguments
			boolean compiledOK = compileProgram(
					compilerArgs.sourceName,
					compilerArgs.objectName,
					compilerArgs.showTree,
					compilerArgs.showTreeAfter,
					compilerArgs.showStats,
					false // Currently, the 'showTable' option is not implemented
			);

			// Exit based on compilation success
			System.exit(compiledOK ? 0 : 1);

		} catch (CommandLine.ParameterException ex) {
			System.err.println(ex.getMessage());
			cmd.usage(System.out);
			System.exit(1);
		}
	}
}