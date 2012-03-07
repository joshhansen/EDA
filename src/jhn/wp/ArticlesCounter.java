package jhn.wp;

import jhn.wp.exceptions.BadLabelException;
import jhn.wp.exceptions.BadWikiTextException;
import jhn.wp.exceptions.RedirectException;
import jhn.wp.exceptions.SkipException;
import jhn.wp.exceptions.TooShortException;
import info.bliki.wiki.filter.ITextConverter;
import info.bliki.wiki.filter.PlainTextConverter;
import info.bliki.wiki.model.WikiModel;
import edu.jhu.nlp.wikipedia.PageCallbackHandler;
import edu.jhu.nlp.wikipedia.WikiPage;
import edu.jhu.nlp.wikipedia.WikiXMLParser;
import edu.jhu.nlp.wikipedia.WikiXMLParserFactory;

public class ArticlesCounter extends CorpusCounter {
	private final String wpdumpFilename;
	
	public ArticlesCounter(String wpdumpFilename) {
		this.wpdumpFilename = wpdumpFilename;
	}

	@Override
	public void count() {
		try {
			super.beforeEverything();
			
			WikiXMLParser wxsp = WikiXMLParserFactory.getSAXParser(wpdumpFilename);
			
			wxsp.setPageCallback(new PageCallbackHandler() {
				int badLabel = 0;
				int redirect = 0;
				int ok = 0;
				int tooShort = 0;
				int total = 0;
				public void process(WikiPage page) {
					total++;
					try {
						final String label = page.getTitle().trim();
						assertLabelOK(label);
						
						final String wikiText = page.getText();
						assertWikiTextOK(wikiText, label);
						
						ok++;
						if(ok % 100 == 0){
							if(ok > 0) {
								System.out.println();
							}
							if(ok % 500 == 0) {
								System.out.println();
								float okPct = (float)ok / (float)total;
								float redirectPct = (float)redirect / (float)total;
								float badLabelPct = (float)badLabel / (float)total;
								float tooShortPct = (float)tooShort / (float)total;
								
								
								System.out.printf("-----%s-----\n", label);
								System.out.printf("ok:%d (%.2f) redirect:%d (%.2f) badLabel:%d (%.2f) tooShort:%d (%.2f) total:%d\n",
										ok, okPct, redirect, redirectPct, badLabel, badLabelPct, tooShort, tooShortPct, total);
							}
						}
						
						ArticlesCounter.this.beforeLabel();
						try {
							ArticlesCounter.this.visitLabel(label);
						} catch (SkipException e) {
							e.printStackTrace();
						}
						System.out.print('.');
//						System.out.println(label);
						
						String text = wikiToText3(wikiText).trim();
//						System.out.println(text);
//						System.out.println();
						
						for(String word : tokenize(text)) {
							ArticlesCounter.this.visitWord(word);
						}
						
						ArticlesCounter.this.afterLabel();
					} catch(RedirectException e) {
						System.err.print('r');
						redirect++;
					} catch(BadWikiTextException e) {
						System.err.print('t');
					} catch(BadLabelException e) {
						System.err.print('l');
						badLabel++;
					} catch (TooShortException e) {
						System.err.print('s');
						tooShort++;
					} catch (SkipException e) {
						e.printStackTrace();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});

			wxsp.parse();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		super.afterEverything();
	}
	
	
	private static final WikiModel wikiModel = new WikiModel("http://www.mywiki.com/wiki/${image}", "http://www.mywiki.com/wiki/${title}");
	private static final ITextConverter conv = new PlainTextConverter();
	private String wikiToText3(String markup) {
        return wikiModel.render(conv, markup);
	}
	
	private static String[] dontStartWithThese = {
		"List of ", "Portal:", "Glossary of ", "Index of ", "Wikipedia:",
		"Category:", "File:", "Template:",
		"Book:", /* collections of pages */
		"MediaWiki:", /* wiki software info */
//		"UN/LOCODE:", /* redirects to locations */
		"Help:", /* wikipedia help */
		"P:", /* redirects to portals */
//		"ISO:", /* redirects to ISO standards */
//		"ISO 639:" /* redirects to languages */
	};
	private void assertLabelOK(String label) throws BadLabelException {
		boolean notOK = false;
		for(String badStart : dontStartWithThese) {
			notOK |= label.startsWith(badStart);
		}
		notOK |= label.contains("(disambiguation)");
		
		if(notOK) throw new BadLabelException(label);
	}
	
	private void assertWikiTextOK(String wikiText, String label) throws BadWikiTextException {
		if(wikiText.startsWith("#REDIRECT")) throw new RedirectException(label);
	}
}
