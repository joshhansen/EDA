package jhn.wp;

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
		WikiXMLParser wxsp = WikiXMLParserFactory.getSAXParser(wpdumpFilename);

		try {
			wxsp.setPageCallback(new PageCallbackHandler() {
				public void process(WikiPage page) {
					System.out.println(page.getTitle());
				}
			});

			wxsp.parse();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	

	protected String wikiToText(String markup) {
//    public static void main(String[] args) throws Exception {
//
//        String markup = "This is ''italic'' and '''that''' is bold. \n"+
//                "=Header 1=\n"+
//                "a list: \n* item A \n* item B \n* item C";

        StringWriter writer = new StringWriter();

        HtmlDocumentBuilder builder = new HtmlDocumentBuilder(writer);
        builder.setEmitAsDocument(false);

        MarkupParser parser = new MarkupParser(new MediaWikiDialect());
        parser.setBuilder(builder);
        parser.parse(markup);

        final String html = writer.toString();
        final StringBuilder cleaned = new StringBuilder();

        HTMLEditorKit.ParserCallback callback = new HTMLEditorKit.ParserCallback() {
                public void handleText(char[] data, int pos) {
                    cleaned.append(new String(data)).append(' ');
                }
        };
        
        try {
			new ParserDelegator().parse(new StringReader(html), callback, false);
		} catch (IOException e) {
			e.printStackTrace();
		}

        System.out.println(markup);
        System.out.println("---------------------------");
        System.out.println(html);
        System.out.println("---------------------------");
        System.out.println(cleaned);
        
        return cleaned.toString();
    }
}
