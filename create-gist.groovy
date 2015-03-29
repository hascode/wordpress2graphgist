def sitemapUrl = 'http://www.hascode.com/sitemap.xml'
def root = new XmlParser().parse(sitemapUrl)
root.children().each { url ->
    println("URL: ${url.loc.text()}")
}
println('finished')
