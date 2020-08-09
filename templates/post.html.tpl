<!DOCTYPE html>
<html lang="en">
    <head>
      {{ import "_styles.html.tpl" }}
    </head>
    <body class="hack">
      <div class="container">
        <header class="header">
          <p><b><a href="/">Home</a> >> <a href="/posts">Posts</a></b></p>
        </header>
        <div>
        {{ .Markdown }}
        </div>
      </div>
    </body>
</html>
