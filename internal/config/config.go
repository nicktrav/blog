package config

import (
	"fmt"
	"io/ioutil"

	"gopkg.in/yaml.v2"
)

type Config struct {
	Server    Server    `yaml:"server"`
	Templates Templates `yaml:"templates"`
	Index     Index     `yaml:"index"`
	Posts     Posts     `yaml:"posts"`
	Useful    Useful    `yaml:"useful"`
}

type Server struct {
	Hostname string `yaml:"hostname"`
	Port     int32  `yaml:"port"`
}

type Templates struct {
	Dir string `yaml:"dir"`
}

type Index struct {
	Template string `yaml:"template"`
}

type Posts struct {
	Dir           string `yaml:"dir"`
	PostTemplate  string `yaml:"post_template"`
	IndexTemplate string `yaml:"index_template"`
}

type Useful struct {
	Dir           string `yaml:"dir"`
	PageTemplate  string `yaml:"page_template"`
	IndexTemplate string `yaml:"index_template"`
}

func (s *Server) Addr() string {
	return fmt.Sprintf("%s:%d", s.Hostname, s.Port)
}

func Parse(path string) (Config, error) {
	var manifest Config

	b, err := ioutil.ReadFile(path)
	if err != nil {
		return manifest, fmt.Errorf("manifest: %s", err)
	}

	err = yaml.Unmarshal(b, &manifest)
	if err != nil {
		return manifest, fmt.Errorf("manifest: %s", err)
	}

	return manifest, nil
}
