package jhn.wp;

import info.bliki.wiki.filter.ITextConverter;
import info.bliki.wiki.filter.PlainTextConverter;
import info.bliki.wiki.model.WikiModel;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

import edu.jhu.nlp.wikipedia.PageCallbackHandler;
import edu.jhu.nlp.wikipedia.WikiPage;
import edu.jhu.nlp.wikipedia.WikiXMLParser;
import edu.jhu.nlp.wikipedia.WikiXMLParserFactory;

import net.java.textilej.parser.MarkupParser;
import net.java.textilej.parser.builder.HtmlDocumentBuilder;
import net.java.textilej.parser.markup.mediawiki.MediaWikiDialect;

public class ArticlesCounter extends CorpusCounter {
	private final String wpdumpFilename;
	
	public ArticlesCounter(String wpdumpFilename) {
		this.wpdumpFilename = wpdumpFilename;
	}

	@Override
	public void count() {
		super.beforeEverything();
		
		WikiXMLParser wxsp = WikiXMLParserFactory.getSAXParser(wpdumpFilename);

		try {
			wxsp.setPageCallback(new PageCallbackHandler() {
				public void process(WikiPage page) {
					ArticlesCounter.this.beforeLabel();
					String label = page.getTitle();
					ArticlesCounter.this.visitLabel(label);
					System.out.println(label);
					
					String text = wikiToText3(page.getText());
					System.out.println(text);
					String[] words = tokenize(text);
					System.out.println();
					for(String word : words) {
						ArticlesCounter.this.visitWord(word);
//						System.out.print(word);
//						System.out.print(' ');
					}
					
					ArticlesCounter.this.afterLabel();
				}
			});

			wxsp.parse();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		super.afterEverything();
	}
	
	

//	protected String wikiToText(String markup) {
//		StringWriter writer = new StringWriter();
//
//		HtmlDocumentBuilder builder = new HtmlDocumentBuilder(writer);
//		builder.setEmitAsDocument(false);
//
//		MarkupParser parser = new MarkupParser(new MediaWikiDialect());
//		parser.setBuilder(builder);
//		parser.parse(markup);
//
//		final String html = writer.toString();
//		String text = htmlToText(html);
//		
//		System.out.println();
//		System.out.println();
//		System.out.println(markup);
//		System.out.println("---------------------------");
//		System.out.println(html);
//		System.out.println("---------------------------");
//		System.out.println(text);
//		System.out.println();
//		System.out.println();
//
//		return text;
//	}
//	
//	private String wikiToText2(String markup) {
//		String html = be.devijver.wikipedia.Parser.toHtml(markup, null);
//		
//		String text = htmlToText(html);
//		System.out.println();
//		System.out.println();
//		System.out.println(markup);
//		System.out.println("---------------------------");
//		System.out.println(html);
//		System.out.println("---------------------------");
//		System.out.println(text);
//		System.out.println();
//		System.out.println();
//
//		return text;
//	}
	
	private static final WikiModel wikiModel = new WikiModel("http://www.mywiki.com/wiki/${image}", "http://www.mywiki.com/wiki/${title}");
	private static final ITextConverter conv = new PlainTextConverter();
	private String wikiToText3(String markup) {
        return wikiModel.render(conv, markup);
	}
	
//	private String htmlToText(String html) {
//		final StringBuilder cleaned = new StringBuilder();
//
//		HTMLEditorKit.ParserCallback callback = new HTMLEditorKit.ParserCallback() {
//			public void handleText(char[] data, int pos) {
//				cleaned.append(new String(data)).append(' ');
//			}
//		};
//
//		try {
//			new ParserDelegator().parse(new StringReader(html), callback, false);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//
//		return cleaned.toString();
//	}
}
