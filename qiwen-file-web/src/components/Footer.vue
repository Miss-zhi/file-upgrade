<template>
	<div class="footer-wrapper">
		<div class="copy-right-wrapper">
			<img
				class="logo"
				:src="logoUrl"
				:alt="$store.getters.imgAlt + 'footerLogo'"
			/>
			<div class="copy-right">
				<span>{{ webSiteName }} {{ copyrightYear }} 版权所有</span>
				<span class="split">|</span>
				<span
					><a
						class="link"
						style="color: white"
						href="http://beian.miit.gov.cn/"
						target="_blank"
						>{{ licenseNo }}</a
					>&nbsp;</span
				>
				<p class="tip-website">
					为获得最佳浏览体验，建议使用IE11、FireFox50.5、Chrome51.0及以上版本的浏览器
				</p>
			</div>
		</div>
	</div>
</template>

<script>
import { getParamsDetail } from '_r/home.js'

export default {
	name: 'Footer',
	data() {
		return {
			logoUrl: require('_a/images/common/logo_footer.png'),
			webSiteName: '网站名称XXX',
			licenseNo: '备案号XXX'
		}
	},
	computed: {
		copyrightYear() {
			return new Date().getFullYear()
		}
	},
	created() {
		this.getParamsDetailData()
	},
	methods: {
		/**
		 * 获取系统参数信息
		 */
		getParamsDetailData() {
			getParamsDetail({ groupName: 'copyright' }).then((res) => {
				if (res.success) {
					this.licenseNo = res.data.licenseKey || '备案号XXX'
					this.webSiteName = res.data.domainChineseName || '网站名称XXX'
				}
			})
		}
	}
}
</script>

<style lang="stylus" scoped>
@import '~_a/styles/varibles.styl';

.footer-wrapper {
  margin-top: 20px;
  display: flex;

  .copy-right-wrapper {
    width: 100%;
    color: $BorderLight;
    background: linear-gradient(to right, $Primary, #66b1ff);
    padding: 16px 0 16px 5vw;
    font-size: 14px;
    color: $BorderLight;
    display: flex;
    align-items: center;

    .logo {
      width: 240px;
      display: block;
      @media screen and (max-width: 920px) {
        width: 160px;
      }
    }

    .split {
      margin: 0 8px;
    }

    .link {
      &:hover {
        text-decoration: underline;
      }
    }

    .tip-website {
      padding-top: 8px;
    }
  }
}
</style>
