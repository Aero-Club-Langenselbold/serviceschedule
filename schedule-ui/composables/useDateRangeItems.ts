export const useDateRangeItems = () => {
  const { t } = useI18n();
  return ref([
    {
      label: t('general.january'),
      value: '01-01'
    }, {
      label: t('general.february'),
      value: '02-01'
    }, {
      label: t('general.march'),
      value: '03-01'
    }, {
      label: t('general.april'),
      value: '04-01'
    }, {
      label: t('general.may'),
      value: '05-01'
    }, {
      label: t('general.june'),
      value: '06-01'
    }, {
      label: t('general.july'),
      value: '07-01'
    }, {
      label: t('general.august'),
      value: '08-01'
    }, {
      label: t('general.september'),
      value: '09-01'
    }, {
      label: t('general.october'),
      value: '10-01'
    }, {
      label: t('general.november'),
      value: '11-01'
    }, {
      label: t('general.december'),
      value: '12-01'
    }
  ])
}
