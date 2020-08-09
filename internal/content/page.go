package content

import (
	"bytes"
	"fmt"
	"io/ioutil"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"strings"

	"github.com/russross/blackfriday/v2"
)

type tags map[string]string

type page struct {
	file     string
	Markdown string
	tags     tags
}

func ParsePages(baseDir string) ([]page, error) {
	r := blackfriday.NewHTMLRenderer(blackfriday.HTMLRendererParameters{
		Flags: blackfriday.CommonHTMLFlags,
	})

	optList := []blackfriday.Option{
		blackfriday.WithRenderer(r),
		blackfriday.WithExtensions(blackfriday.CommonExtensions),
	}

	var pages []page
	err := filepath.Walk(baseDir, func(path string, info os.FileInfo, err error) error {
		// Skip anything that doesn't look like a markdown file.
		if info.IsDir() || !strings.HasSuffix(path, ".md") {
			return nil
		}

		// Else, parse this Markdown file.
		rawMd, err := ioutil.ReadFile(path)
		if err != nil {
			return fmt.Errorf("post: %s", err)
		}

		parser := blackfriday.New(optList...)
		ast := parser.Parse(rawMd)

		var metadataTable *blackfriday.Node
		var buf bytes.Buffer
		ast.Walk(func(node *blackfriday.Node, entering bool) blackfriday.WalkStatus {
			// The first table we encounter will be the metadata table.  Strip
			// the table from the output but save the pointer to the node so
			// that it can be processed in a subsequent step to fetch the
			// metadata.
			if metadataTable == nil && entering && node.Type == blackfriday.Table {
				metadataTable = node
				return blackfriday.SkipChildren
			}
			return r.RenderNode(&buf, node, entering)
		})

		file, err := filepath.Rel(baseDir, path)
		if err != nil {
			return fmt.Errorf("page: %s", err)
		}

		tags := parseTags(metadataTable)
		pages = append(pages, page{
			file:     file,
			tags:     tags,
			Markdown: buf.String(),
		})
		return nil
	})

	if err != nil {
		return nil, err
	}

	return pages, nil
}

func parseTags(node *blackfriday.Node) tags {
	res := make(tags)

	var key string
	isKey := true
	node.Walk(func(node *blackfriday.Node, entering bool) blackfriday.WalkStatus {
		if node.Type == blackfriday.TableHead && entering {
			return blackfriday.SkipChildren
		}

		if node.Type == blackfriday.Text {
			if isKey {
				key = string(node.Literal)
			} else {
				res[key] = string(node.Literal)
			}
			isKey = !isKey
		}

		return 0
	})

	return res
}

type pageIndex map[string]page

type PageManager struct {
	templater     *Templater
	idx           pageIndex
	pageTemplate  string
	indexTemplate string
}

type PageManagerOpts struct {
	Dir           string
	PageTemplate  string
	IndexTemplate string
}

func NewPageManager(templater *Templater, opts PageManagerOpts) (*PageManager, error) {
	pages, err := ParsePages(opts.Dir)
	if err != nil {
		return nil, err
	}

	idx := make(pageIndex)
	for _, p := range pages {
		name, ok := p.tags["name"]
		if !ok {
			return nil, fmt.Errorf("post: tag 'name' not found for page %s", p.file)
		}
		idx[name] = p
	}

	return &PageManager{
		idx:           idx,
		templater:     templater,
		pageTemplate:  opts.PageTemplate,
		indexTemplate: opts.IndexTemplate,
	}, nil
}

func (p *PageManager) NewPageHandler() http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		name := r.URL.Path
		post, ok := p.idx[name]
		if !ok {
			http.NotFound(w, r)
			return
		}

		err := p.templater.Render(w, p.pageTemplate, post)
		if err != nil {
			log.Printf("error: %s\n", err)
			w.WriteHeader(http.StatusInternalServerError)
			return
		}
	}
}

type indexPageItem struct {
	Name string
}

type indexPage struct {
	Pages []indexPageItem
}

func (p *PageManager) NewIndexHandler() (http.HandlerFunc, error) {
	var pages []indexPageItem
	for _, v := range p.idx {
		name, ok := v.tags["name"]
		if !ok {
			return nil, fmt.Errorf("page: 'name' tag not found for %q", v.file)
		}
		pages = append(pages, indexPageItem{Name: name})
	}

	return func(w http.ResponseWriter, r *http.Request) {
		err := p.templater.Render(w, p.indexTemplate, indexPage{Pages: pages})
		if err != nil {
			log.Printf("error: %s\n", err)
			w.WriteHeader(http.StatusInternalServerError)
			return
		}
	}, nil
}
