module.exports = {
  title: '歩く aruku',
  tagline: 'A Random Walk Engine for Apache Spark',
  url: 'https://pierrenodet.github.io/aruku',
  baseUrl: '/aruku/',
  favicon: 'img/favicon.ico',
  organizationName: 'pierrenodet',
  projectName: 'aruku',
  themeConfig: {
    navbar: {
      title: '歩く aruku',
      links: [
        { href: '/aruku/api/index.html', label: 'API', position: 'right', target: '_parent' },
        { to: 'docs/overview', label: 'Documentation', position: 'right' },
        {
          href: 'https://github.com/pierrenodet/aruku',
          label: 'GitHub',
          position: 'right',
          target: '_self'
        },
      ],
    },
    footer: {
      copyright: `Copyright © ${new Date().getFullYear()} Pierre Nodet`,
    },
    disableDarkMode: true,
    prism: {
      theme: require('prism-react-renderer/themes/nightOwl'),
      additionalLanguages: ['java', 'scala'],
    }
  },
  presets: [
    [
      '@docusaurus/preset-classic',
      {
        docs: {
          path: "../modules/aruku-docs/target/mdoc",
          routeBasePath: "docs",
          include: ['*.md', '*.mdx'],
          sidebarPath: require.resolve('./sidebars.js'),
          editUrl:
            'https://github.com/pierrenodet/aruku/edit/master',
        },
        theme: {
          customCss: require.resolve('./src/css/custom.css'),
        },
      },
    ],
  ],
};
