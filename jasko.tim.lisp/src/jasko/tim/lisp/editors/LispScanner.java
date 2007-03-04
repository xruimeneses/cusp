package jasko.tim.lisp.editors;

import jasko.tim.lisp.ColorManager;

import org.eclipse.jface.text.*;
import org.eclipse.jface.text.rules.*;
import org.eclipse.swt.*;
import org.eclipse.swt.graphics.Color;

public class LispScanner extends RuleBasedScanner {

	public LispScanner(ColorManager manager, Color background) {
		IRule[] rules = new IRule[8];
		
		IToken number = new Token(
			new TextAttribute(manager.getColor(ColorManager.NUMBER), background, SWT.NORMAL));
		rules[0] = new NumberRule(number);
		
		IToken comment = new Token(
			new TextAttribute(manager.getColor(ColorManager.PAREN), background, SWT.NORMAL));
		rules[1] = new EndOfLineRule(";", comment);
		
		IToken paren = new Token(
			new TextAttribute(manager.getColor(ColorManager.PAREN), background, SWT.NORMAL));
		rules[2] = new WordRule(new ParenDetector(), paren);
		
		IToken symbol = new Token(
			new TextAttribute(manager.getColor(ColorManager.SYMBOL), background, SWT.NORMAL));
		rules[3] = new WordRule(new SymbolDetector(':'), symbol);
		
		IToken params = new Token(
			new TextAttribute(manager.getColor(ColorManager.PARAMS), background, SWT.ITALIC));
		rules[4] = new WordRule(new SymbolDetector('&'), params);
		
		IToken global = new Token(
			new TextAttribute(manager.getColor(ColorManager.GLOBAL), background, SWT.NORMAL));
		//rules[5] = new SingleLineRule("*", "*", global);
		rules[5] = new WordPatternRule(new LispIdentifierDetector(), "*", "*", global);
		
		IToken constant = new Token(
				new TextAttribute(manager.getColor(ColorManager.CONSTANT), background, SWT.NORMAL));
		rules[6] = new WordPatternRule(new LispIdentifierDetector(), "+", "+", constant);
		
		IToken defaultToken = new Token(
				new TextAttribute(manager.getColor(ColorManager.KEYWORD), background, SWT.NORMAL));
		IToken keyword = new Token(
			new TextAttribute(manager.getColor(ColorManager.KEYWORD), background, SWT.BOLD));
		//WordRule keywordRule = new WordRule(new LispSpecialWordDetector(), defaultToken);
		WordRule keywordRule = new WordRule(new LispIdentifierDetector(), defaultToken);
		for(int i = 0; i < LispSpecialWordDetector.RESERVED_WORDS.length; i++) {
			keywordRule.addWord(LispSpecialWordDetector.RESERVED_WORDS[i], keyword);
		}
		rules[7] = keywordRule;
		
		
		// Add generic whitespace rule.
		//rules[7] = new WhitespaceRule(new LispWhitespaceDetector());

		setRules(rules);
		setDefaultReturnToken(defaultToken);
	}
	
	
	
	
}