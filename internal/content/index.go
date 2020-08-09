package content

import (
	"log"
	"net/http"
)

type IndexController struct {
	templater *Templater
	template  string
}

func NewIndexController(templater *Templater, template string) *IndexController {
	return &IndexController{
		templater: templater,
		template:  template,
	}
}

func (i *IndexController) NewHandler() http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		err := i.templater.Render(w, i.template, struct{}{})
		if err != nil {
			log.Printf("error: %s\n", err)
			w.WriteHeader(http.StatusInternalServerError)
			return
		}
	}
}
