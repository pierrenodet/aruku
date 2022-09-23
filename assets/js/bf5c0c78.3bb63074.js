"use strict";(self.webpackChunkaruku=self.webpackChunkaruku||[]).push([[762],{3905:function(e,n,t){t.d(n,{Zo:function(){return s},kt:function(){return k}});var r=t(7294);function a(e,n,t){return n in e?Object.defineProperty(e,n,{value:t,enumerable:!0,configurable:!0,writable:!0}):e[n]=t,e}function o(e,n){var t=Object.keys(e);if(Object.getOwnPropertySymbols){var r=Object.getOwnPropertySymbols(e);n&&(r=r.filter((function(n){return Object.getOwnPropertyDescriptor(e,n).enumerable}))),t.push.apply(t,r)}return t}function l(e){for(var n=1;n<arguments.length;n++){var t=null!=arguments[n]?arguments[n]:{};n%2?o(Object(t),!0).forEach((function(n){a(e,n,t[n])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(t)):o(Object(t)).forEach((function(n){Object.defineProperty(e,n,Object.getOwnPropertyDescriptor(t,n))}))}return e}function i(e,n){if(null==e)return{};var t,r,a=function(e,n){if(null==e)return{};var t,r,a={},o=Object.keys(e);for(r=0;r<o.length;r++)t=o[r],n.indexOf(t)>=0||(a[t]=e[t]);return a}(e,n);if(Object.getOwnPropertySymbols){var o=Object.getOwnPropertySymbols(e);for(r=0;r<o.length;r++)t=o[r],n.indexOf(t)>=0||Object.prototype.propertyIsEnumerable.call(e,t)&&(a[t]=e[t])}return a}var u=r.createContext({}),c=function(e){var n=r.useContext(u),t=n;return e&&(t="function"==typeof e?e(n):l(l({},n),e)),t},s=function(e){var n=c(e.components);return r.createElement(u.Provider,{value:n},e.children)},d={inlineCode:"code",wrapper:function(e){var n=e.children;return r.createElement(r.Fragment,{},n)}},p=r.forwardRef((function(e,n){var t=e.components,a=e.mdxType,o=e.originalType,u=e.parentName,s=i(e,["components","mdxType","originalType","parentName"]),p=c(t),k=a,f=p["".concat(u,".").concat(k)]||p[k]||d[k]||o;return t?r.createElement(f,l(l({ref:n},s),{},{components:t})):r.createElement(f,l({ref:n},s))}));function k(e,n){var t=arguments,a=n&&n.mdxType;if("string"==typeof e||a){var o=t.length,l=new Array(o);l[0]=p;var i={};for(var u in n)hasOwnProperty.call(n,u)&&(i[u]=n[u]);i.originalType=e,i.mdxType="string"==typeof e?e:a,l[1]=i;for(var c=2;c<o;c++)l[c]=t[c];return r.createElement.apply(null,l)}return r.createElement.apply(null,t)}p.displayName="MDXCreateElement"},8622:function(e,n,t){t.r(n),t.d(n,{assets:function(){return s},contentTitle:function(){return u},default:function(){return k},frontMatter:function(){return i},metadata:function(){return c},toc:function(){return d}});var r=t(7462),a=t(3366),o=(t(7294),t(3905)),l=["components"],i={id:"walks",title:"Provided Walks"},u=void 0,c={unversionedId:"walks",id:"walks",title:"Provided Walks",description:"As of now three random walks are provided with aruku",source:"@site/../modules/aruku-docs/target/mdoc/walks.md",sourceDirName:".",slug:"/walks",permalink:"/aruku/docs/walks",draft:!1,editUrl:"https://github.com/pierrenodet/aruku/edit/master/../modules/aruku-docs/target/mdoc/walks.md",tags:[],version:"current",frontMatter:{id:"walks",title:"Provided Walks"},sidebar:"someSidebar",previous:{title:"Transition Probability",permalink:"/aruku/docs/transition"},next:{title:"Example",permalink:"/aruku/docs/example"}},s={},d=[{value:"Personalized Page Rank",id:"personalized-page-rank",level:2},{value:"DeepWalk",id:"deepwalk",level:2},{value:"node2vec",id:"node2vec",level:2}],p={toc:d};function k(e){var n=e.components,t=(0,a.Z)(e,l);return(0,o.kt)("wrapper",(0,r.Z)({},p,t,{components:n,mdxType:"MDXLayout"}),(0,o.kt)("p",null,"As of now three random walks are provided with aruku"),(0,o.kt)("h2",{id:"personalized-page-rank"},"Personalized Page Rank"),(0,o.kt)("pre",null,(0,o.kt)("code",{parentName:"pre",className:"language-scala",metastring:'title="modules/aruku/walks/PersonalizedPageRank.scala"',title:'"modules/aruku/walks/PersonalizedPageRank.scala"'},"case object PersonalizedPageRank {\n\n  def config(numWalkers: Long)\n  def transition(pi: Double)\n\n}\n")),(0,o.kt)("h2",{id:"deepwalk"},"DeepWalk"),(0,o.kt)("pre",null,(0,o.kt)("code",{parentName:"pre",className:"language-scala",metastring:'title="modules/aruku/walks/DeepWalk.scala"',title:'"modules/aruku/walks/DeepWalk.scala"'},"case object DeepWalk {\n\n  def config(numWalkers: Long)\n  def transition(walkLength: Long)\n\n}\n")),(0,o.kt)("h2",{id:"node2vec"},"node2vec"),(0,o.kt)("pre",null,(0,o.kt)("code",{parentName:"pre",className:"language-scala",metastring:'title="modules/aruku/walks/Node2Vec.scala"',title:'"modules/aruku/walks/Node2Vec.scala"'},"object Node2Vec {\n\n  def config(numWalkers: Long)\n  def transition(p: Double, q: Double, walkLength: Long)\n\n}\n")))}k.isMDXComponent=!0}}]);